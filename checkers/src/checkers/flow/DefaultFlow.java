package checkers.flow;

import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;

import checkers.basetype.BaseTypeChecker;
import checkers.nullness.quals.Pure;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.AnnotationUtils;
import checkers.util.ElementUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;
import checkers.nullness.quals.*;

import com.sun.source.tree.*;

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

        if (debug != null) {
            debug.println("Flow: newVar(" + tree + ") -- " + type);
            debug.println("  flowState before newVar: " + flowState);
        }

        // Determine the initial status of the variable by checking its
        // annotated type.
        for (AnnotationMirror annotation : this.flowState.annotations) {
            if (type.hasAnnotation(annotation))
                flowState.annos.set(annotation, idx);
            else
                flowState.annos.clear(annotation, idx);
        }

        if (debug != null) {
            debug.println("  flowState after newVar: " + flowState);
        }
    }

    @Override
    protected void propagate(Tree lhs, ExpressionTree rhs) {
        if (debug != null) {
            debug.println("Flow: try propagate from rhs: " + rhs + " into lhs: " + lhs);
            debug.println("  flowState before propagate: " + flowState);
        }

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

        Set<AnnotationMirror> typeAnnos;    // rhs
        Set<AnnotationMirror> eltTypeAnnos; // lhs

        if ((type.getKind() == TypeKind.TYPEVAR ||
                type.getKind() == TypeKind.WILDCARD) &&
                (eltType.getKind() != TypeKind.TYPEVAR &&
                eltType.getKind() != TypeKind.WILDCARD)) {
            // Only take the effective upper bound if the LHS
            // is not also a type variable/wildcard
            typeAnnos = type.getEffectiveAnnotations();
        } else {
            typeAnnos = type.getAnnotations();
        }
        eltTypeAnnos = eltType.getAnnotations();

        if (!eltTypeAnnos.isEmpty() && !typeAnnos.isEmpty()
                && !annoRelations.isSubtype(typeAnnos, eltTypeAnnos)) {
            return;
        }

        for (AnnotationMirror annotation : this.flowState.annotations) {
            AnnotationMirror annotationRoot = annoRelations.getTopAnnotation(annotation);
            // Propagate/clear the annotation if it's annotated or an annotation
            // had been inferred previously.
            if (AnnotationUtils.containsSame(typeAnnos, annotation) && !eltTypeAnnos.isEmpty()
                    && annoRelations.isSubtype(typeAnnos, eltTypeAnnos)) {
                // to ensure that there is always just one annotation set, we
                // first clear the annotation that was previously used (if in the same hierarchy)
                for (AnnotationMirror other : this.flowState.annotations) {
                    if (annoRelations.isSubtype(other, annotationRoot)) {
                        flowState.annos.clear(other, idx);
                    }
                }
                flowState.annos.set(annotation, idx);
            } else if (rIdx >= 0 && flowState.annos.get(annotation, rIdx)) {
                flowState.annos.set(annotation, idx);
            } else {
                flowState.annos.clear(annotation, idx);
            }
        }

        Set<AnnotationMirror> remainder = AnnotationUtils.createAnnotationSet();
        // Find all of the annotations in typeAnnos that are not included in flowState.annotations.
        for (AnnotationMirror anno : typeAnnos) {
            if (!AnnotationUtils.containsSame(this.flowState.annotations, anno)) {
                remainder.add(anno);
            }
        }

        for (AnnotationMirror anno : remainder) {
            // If this annotation is the same as a supported annotation but with a different value
            // then add it to the supported annotations. This is to support annotations with
            // values.
            if (AnnotationUtils.containsSameIgnoringValues(this.flowState.annotations, anno)) {
                this.flowState.annotations.add(anno);
                this.flowState.annos.set(anno, idx);
            }
        }

        // just to make sure everything worked correctly
        flowState.annos.valid();

        if (debug != null)
            debug.println("  flowState after propagate: " + flowState);
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
            if (elt.asType().getKind()!=TypeKind.TYPEVAR && rhs.hasEffectiveAnnotation(annotation)) {
                // Only take the effective annotations if the LHS is not a type variable
                flowState.annos.set(annotation, idx);
            } else if (rhs.hasAnnotation(annotation)) {
                flowState.annos.set(annotation, idx);
            } else {
                flowState.annos.clear(annotation, idx);
            }
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
                if (debug != null) {
                    debug.println("Flow: recordBits(" + tree + ") + " + annotation + " "
                            + flowState.annos.get(annotation, idx) + " as " + tree.getKind());
                }
                if (flowState.annos.get(annotation, idx)) {
                    Set<AnnotationMirror> existing = flowResults.get(tree);

                    // Don't replace the existing annotation unless the current
                    // annotation is *more* specific than the existing one.
                    if (existing == null) {
                        addFlowResult(flowResults, tree, annotation);
                    } else {
                        AnnotationMirror annotationRoot = annoRelations.getTopAnnotation(annotation);
                        boolean inExisting = false;
                        for (AnnotationMirror ex : existing) {
                            if (annoRelations.isSubtype(ex, annotationRoot)) {
                                inExisting = true;
                            }
                            if (annoRelations.isSubtype(ex, annotation)) {
                                // TODO: isn't this logic inverted here? Shouldn't we test the other direction?
                                addFlowResult(flowResults, tree, annotation);
                            }
                        }
                        if (!inExisting) {
                            addFlowResult(flowResults, tree, annotation);
                        }
                    }
                } else {
                    Set<AnnotationMirror> exists = flowResults.get(tree);
                    if (exists!=null && exists.contains(annotation)) {
                        // We inferred an annotation in this location that is not
                        // applicable anymore
                        // occurs in loop where an assignment invalidates the
                        // condition in the next round
                        removeFlowResult(flowResults, tree, annotation);
                    }
                }
            }
        }
    }

    @Override
    protected void clearOnCall(/*@Nullable*/ MethodTree enclMeth, ExecutableElement method) {
        final String methodPackage = ElementUtils.enclosingPackage(method).getQualifiedName().toString();
        boolean isJDKMethod = methodPackage.startsWith("java")
                || methodPackage.startsWith("com.sun");
        boolean isPure = factory.getDeclAnnotation(method, Pure.class) != null;
        if (!isPure) {
            for (int i = 0; i < this.flowState.vars.size(); i++) {
                Element var = this.flowState.vars.get(i);
                for (AnnotationMirror a : this.flowState.annotations) {
                    if (!isJDKMethod && isNonFinalField(var)
                            && !varDefHasAnnotation(enclMeth, a, var)) {
                        flowState.annos.clear(a, i);
                    }
                }
            }
        }
    }

}
