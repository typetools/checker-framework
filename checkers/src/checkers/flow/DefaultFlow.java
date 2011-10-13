package checkers.flow;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import checkers.basetype.BaseTypeChecker;
import checkers.nullness.quals.Pure;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypes;
import checkers.util.ElementUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * The default implementation of the flow-sensitive type inference.
 *
 * @param <ST> the flow state subclass that should be used.
 *   If DefaultFlow is instantiated directly the type argument has to be DefaultFlowState.
 *   In other cases, method createFlowState has to be overridden correctly!
 */
public class DefaultFlow<ST extends DefaultFlowState> extends AbstractFlow<ST> {

    public DefaultFlow(BaseTypeChecker checker, CompilationUnitTree root,
            Set<AnnotationMirror> annotations, AnnotatedTypeFactory factory) {
        super(checker, root, annotations, factory);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ST createFlowState(Set<AnnotationMirror> annotations) {
        return (ST) new DefaultFlowState(annotations);
    }

    @Override
    protected void newVar(VariableTree tree) {
        VariableElement var = TreeUtils.elementFromDeclaration(tree);
        assert var != null : "no symbol from tree";

        if (this.flowState.vars.contains(var)) {
            if (debug != null)
                debug.println("Flow: newVar(" + tree + ") reusing index");
            return;
        }

        int idx = this.flowState.vars.size();
        this.flowState.vars.add(var);

        AnnotatedTypeMirror type = factory.getAnnotatedType(tree);
        assert type != null : "no type from symbol";

        if (debug != null)
            debug.println("Flow: newVar(" + tree + ") -- " + type);

        // Determine the initial status of the variable by checking its
        // annotated type.
        for (AnnotationMirror annotation : this.flowState.annotations) {
            if (hasAnnotation(type, annotation))
                flowState.annos.set(annotation, idx);
            else
                flowState.annos.clear(annotation, idx);
        }
    }

    @Override
    protected void propagate(Tree lhs, ExpressionTree rhs) {

        if (debug != null)
            debug.println("Flow: try propagate from " + rhs);

        // Skip assignment to arrays.
        if (lhs.getKind() == Tree.Kind.ARRAY_ACCESS)
            return;

        // Get the element for the left-hand side.
        Element elt = InternalUtils.symbol(lhs);
        assert elt != null;
        AnnotatedTypeMirror eltType = factory.getAnnotatedType(elt);

        // Get the annotated type of the right-hand side.
        AnnotatedTypeMirror type = factory.getAnnotatedType(rhs);
        if (TreeUtils.skipParens(rhs).getKind() == Tree.Kind.ARRAY_ACCESS) {
            propagateFromType(lhs, type);
            return;
        }
        assert type != null;

        int idx = this.flowState.vars.indexOf(elt);
        if (idx < 0)
            return;

        // Get the element for the right-hand side.
        Element rElt = InternalUtils.symbol(rhs);
        int rIdx = this.flowState.vars.indexOf(rElt);

        Set<AnnotationMirror> typeAnnos = type.getEffectiveAnnotations();
        Set<AnnotationMirror> eltTypeAnnos = eltType.getEffectiveAnnotations();

        if (eltType.isAnnotated()
                && type.isAnnotated()
                && !annoRelations.isSubtype(typeAnnos, eltTypeAnnos)) {
            return;
        }

        for (AnnotationMirror annotation : this.flowState.annotations) {
            // Propagate/clear the annotation if it's annotated or an annotation
            // had been inferred previously.
            if (hasAnnotation(type, annotation)
                    && annoRelations.isSubtype(typeAnnos, eltTypeAnnos)) {
                flowState.annos.set(annotation, idx);
                // to ensure that there is always just one annotation set, we
                // clear the annotation that was previously used
                // for (AnnotationMirror oldsuper : eltType.getAnnotations()) {
                for (AnnotationMirror other : this.flowState.annotations) {
                    if (!other.equals(annotation)
                            && flowState.annos.contains(other)) {
                        // The get is not necessary and might observe annos in
                        // an invalid state.
                        // annos.get(other, idx)
                        flowState.annos.clear(other, idx);
                    }
                }
            } else if (rIdx >= 0 && flowState.annos.get(annotation, rIdx)) {
                flowState.annos.set(annotation, idx);
            } else {
                flowState.annos.clear(annotation, idx);
            }
        }
        // just to make sure everything worked correctly
        flowState.annos.valid();
    }

    @Override
    void propagateFromType(Tree lhs, AnnotatedTypeMirror rhs) {

        if (lhs.getKind() == Tree.Kind.ARRAY_ACCESS)
            return;

        Element elt = InternalUtils.symbol(lhs);

        int idx = this.flowState.vars.indexOf(elt);
        if (idx < 0) return;

        // WMD: if we're setting something, can the GenKillBits invariant be violated?
        for (AnnotationMirror annotation : this.flowState.annotations) {
            if (hasAnnotation(rhs, annotation))
                flowState.annos.set(annotation, idx);
            else
                flowState.annos.clear(annotation, idx);
        }
    }

    @Override
    protected void recordBitsImps(Tree tree, Element elt) {

        int idx = this.flowState.vars.indexOf(elt);
        // If the variable has not been previously encountered, add it to the
        // list of variables. (We can't use newVar here since we don't have the
        // declaration tree.)
        if (idx < 0 && elt instanceof VariableElement) {
            idx = this.flowState.vars.size();
            this.flowState.vars.add((VariableElement)elt);
        }

        if (idx >= 0) {
            for (AnnotationMirror annotation : this.flowState.annotations) {
                if (debug != null)
                    debug.println("Flow: recordBits(" + tree + ") + " + annotation + " "
                            + flowState.annos.get(annotation, idx) + " as " + tree.getKind());
                if (flowState.annos.get(annotation, idx)) {
                    AnnotationMirror existing = flowResults.get(tree);

                    // Don't replace the existing annotation unless the current
                    // annotation is *more* specific than the existing one.
                    if (existing == null || annoRelations.isSubtype(existing, annotation))
                        flowResults.put(tree, annotation);
                } else if (flowResults.get(tree) == annotation) {
                    // We inferred an annotation in this location that is not
                    // applicable anymore
                    // occurs in loop where an assignment invalidates the
                    // condition in the next round
                    flowResults.remove(tree);
                }
            }
        }
    }

    @Override
    protected void clearOnCall(ExecutableElement method) {
        final String methodPackage = ElementUtils.enclosingPackage(method).getQualifiedName().toString();
        boolean isJDKMethod = methodPackage.startsWith("java")
                || methodPackage.startsWith("com.sun");
        boolean isPure = factory.getDeclAnnotation(method, Pure.class) != null;
        for (int i = 0; i < this.flowState.vars.size(); i++) {
            Element var = this.flowState.vars.get(i);
            for (AnnotationMirror a : this.flowState.annotations)
                if (!isJDKMethod && isNonFinalField(var)
                        && !varDefHasAnnotation(a, var) && !isPure)
                    flowState.annos.clear(a, i);
        }
    }

}
