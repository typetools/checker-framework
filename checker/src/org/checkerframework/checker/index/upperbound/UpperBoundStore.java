package org.checkerframework.checker.index.upperbound;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;

/**
 * At every possible side effect, let T be all the types (for method formals and returns of any
 * method in the enclosing class, and anywhere in the store) whose expression might have its value
 * affected by the side effect.
 *
 * <p>If the side effect is a reassignment to a local variable, arr1, these are only expressions
 * that include any of:
 *
 * <ul>
 *   <li>arr1
 * </ul>
 *
 * <p>If the side effect is a reassignment to an array field, arr1, these are expressions that
 * include any of:
 *
 * <ul>
 *   <li>arr1
 *   <li>a method call
 * </ul>
 *
 * <p>If the side effect is a reassignment to a non-array reference (non-primitive) field or a call
 * to a non-side-effect-free method, these are expressions that include any of:
 *
 * <ul>
 *   <li>a non-final field whose type is not an array
 *   <li>a method call
 * </ul>
 *
 * <p>If the side effect is a reassignment to a primitive field, no expressions are affected.
 *
 * <p>Let V be all the variables with a type in T. In particular, for a reassignment “arr1 = …”, V
 * includes every int variable with type LT[E]L("...arr1...").
 *
 * <p>For every variable v in V: If v is in the refinement store, then unrefine the int variable.
 * (No need to check the final unrefined type; it will be handled by the next rule if it still has
 * type @LT[E]L).).
 *
 * <p>For every type t in T: If t is not a refined type (not from the store), then it is a
 * user-written type such as @LT[E]L("...arr1...") or a possible alias. The type-checker issues an
 * error, stating that this is an illegal assignment. The type-checker suggests that the user should
 * either make the array variable final or (if the possible reassignment was a method call) annotate
 * the method as @SideEffectFree.
 */
public class UpperBoundStore extends CFAbstractStore<CFValue, UpperBoundStore> {

    private static /*@CompilerMessageKey*/ String NO_REASSIGN = "reassignment.not.permitted";
    private static /*@CompilerMessageKey*/ String NO_REASSIGN_FIELD =
            "reassignment.field.not.permitted";
    private static /*@CompilerMessageKey*/ String SIDE_EFFECTING_METHOD =
            "side.effect.invalidation";
    private static /*@CompilerMessageKey*/ String NO_REASSIGN_FIELD_METHOD =
            "reassignment.field.not.permitted.method";
    private SourceChecker checker;

    private enum CheckController {
        NAME_ONLY,
        NAME_AND_METHOD_CALLS,
        NONFINAL_REFS_AND_METHOD_CALLS,
        NONFINAL_REFS_AND_METHOD_CALLS_METHOD
    }

    public UpperBoundStore(UpperBoundAnalysis analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
        checker = analysis.getChecker();
    }

    public UpperBoundStore(
            UpperBoundAnalysis analysis, CFAbstractStore<CFValue, UpperBoundStore> other) {
        super(other);
        checker = analysis.getChecker();
    }

    @Override
    public void updateForMethodCall(
            MethodInvocationNode n, AnnotatedTypeFactory factory, CFValue val) {
        super.updateForMethodCall(n, factory, val);

        ExecutableElement elt = n.getTarget().getMethod();

        if (!isSideEffectFree(factory, elt)) {
            List<? extends Element> enclosedElts =
                    ElementUtils.enclosingClass(elt).getEnclosedElements();
            List<AnnotatedTypeMirror> enclosedTypes = new ArrayList<AnnotatedTypeMirror>();

            findEnclosedTypes(enclosedTypes, enclosedElts);

            // Should include all method calls and non-array references, but not the name of the method.
            clearFromStore("", n, CheckController.NONFINAL_REFS_AND_METHOD_CALLS_METHOD);
            checkForRemainingAnnotations(
                    enclosedTypes, "", n, CheckController.NONFINAL_REFS_AND_METHOD_CALLS_METHOD);
        }
    }

    @Override
    public void updateForAssignment(Node n, CFValue val) {
        // Do reassignment things here.
        super.updateForAssignment(n, val);

        // This code determines the list of dependences in types that are to be invalidated
        CheckController checkController = null;

        if (n.getType().getKind() == TypeKind.ARRAY) {
            if (n instanceof LocalVariableNode) {
                // Do not warn about assigning to a final variable. javac handles this.
                if (!ElementUtils.isEffectivelyFinal(((LocalVariableNode) n).getElement())) {
                    checkController = CheckController.NAME_ONLY;
                }
            }
            if (n instanceof FieldAccessNode) {
                // Do not warn about assigning to a final field. javac handles this.
                if (!ElementUtils.isEffectivelyFinal(((FieldAccessNode) n).getElement())) {
                    checkController = CheckController.NAME_AND_METHOD_CALLS;
                }
            }
        } else {
            if (n instanceof FieldAccessNode) {
                if (!n.getType().getKind().isPrimitive()) {
                    if (!ElementUtils.isEffectivelyFinal(((FieldAccessNode) n).getElement())) {
                        checkController = CheckController.NONFINAL_REFS_AND_METHOD_CALLS;
                    }
                }
            }
        }

        // Find all possibly-invalidated types
        if (checkController != null) {
            Element elt;
            // So that assignments into arrays are treated correctly, as well as type casts
            if (n instanceof FieldAccessNode) {
                elt = ((FieldAccessNode) n).getElement();
            } else if (n instanceof LocalVariableNode) {
                elt = ((LocalVariableNode) n).getElement();
            } else {
                return; // can't get an element, so there's nothing to do.
            }

            List<? extends Element> enclosedElts =
                    ElementUtils.enclosingClass(elt).getEnclosedElements();
            List<AnnotatedTypeMirror> enclosedTypes = new ArrayList<AnnotatedTypeMirror>();

            findEnclosedTypes(enclosedTypes, enclosedElts);

            FlowExpressions.Receiver rec =
                    FlowExpressions.internalReprOf(analysis.getTypeFactory(), n);
            String canonicalTargetName = rec.toString();

            clearFromStore(canonicalTargetName, n, checkController);
            checkForRemainingAnnotations(enclosedTypes, canonicalTargetName, n, checkController);
        }
    }

    private void findEnclosedTypes(
            List<AnnotatedTypeMirror> enclosedTypes, List<? extends Element> enclosedElts) {
        for (Element e : enclosedElts) {
            AnnotatedTypeMirror atm = analysis.getTypeFactory().getAnnotatedType(e);
            enclosedTypes.add(atm);
            if (e.getKind() == ElementKind.METHOD) {
                ExecutableElement ee = (ExecutableElement) e;
                List<? extends Element> rgparam = ee.getParameters();
                for (Element param : rgparam) {
                    AnnotatedTypeMirror atmP = analysis.getTypeFactory().getAnnotatedType(param);
                    enclosedTypes.add(atmP);
                }
            }
        }
    }

    /**
     * This function handles the meat of checking whether an annotation is invalid. If the
     * annotation is invalid (i.e. it should be unrefined or it should result in a warning) then the
     * return Results is a failure; Otherwise, it returns a success.
     *
     * <p>{@code anno} is the annotation to be checked. If it is not dependent, success is returned.
     * Otherwise, an UBQualifier is created, and all the things it depends on (as Strings) are
     * collected and canonicalized.
     *
     * <p>Then, the appropriate checks are made based on the {@code checkController} the result is
     * returned.
     *
     * <p>There are three possible checks:
     *
     * <ul>
     *   <li>Does the annotation depend on the {@code canonicalTargetName} parameter?
     *   <li>Does the annotation depend on any non-final references?
     *   <li>Does the annotation include any method calls?
     * </ul>
     *
     * <p>The CheckController contains an option for just the first, an option for the first and
     * second, and an option for the second and third. These are the only possible sets of
     * invalidations (according to the document that is reproduced as the JavaDoc on this class).
     *
     * <p>The CheckController also contains information that is used in deciding which report is
     * issued: it contains separate options for its third option (i.e. the second and third items in
     * the list) so that different messages can be issued for field reassignment invalidation and
     * invalidation because of a call to a non-side-effect free method.
     */
    private Result checkAnno(
            AnnotationMirror anno,
            String canonicalTargetName,
            Node n,
            CheckController checkController) {
        UpperBoundAnnotatedTypeFactory factory =
                (UpperBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        if (!AnnotationUtils.hasElementValue(anno, "value")) {
            return Result.SUCCESS;
        }
        UBQualifier.LessThanLengthOf qual =
                (UBQualifier.LessThanLengthOf) UBQualifier.createUBQualifier(anno);

        List<String> dependencies = new ArrayList<>();
        for (String s : qual.getArrays()) {
            dependencies.add(s);
        }
        dependencies.addAll(qual.getOffsetsAsStrings());

        List<String> canonicalDependencies = new ArrayList<>();
        for (String s : dependencies) {
            FlowExpressions.Receiver r = null;
            try {
                TreePath path = factory.getPath(n.getTree());
                if (path != null) {
                    r = factory.getReceiverFromJavaExpressionString(s, path);
                }

            } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {
            }
            if (r == null) {
                return Result.SUCCESS;
            }
            canonicalDependencies.add(r.toString());
            while (r instanceof FlowExpressions.FieldAccess) {
                // I included an exception here for "this", because otherwise the rules don't make sense - any
                // field, including final ones, would be invalidated, since it's impossible to know if this is
                // final or not. But this can't be modified by calling other code, so it's fine.
                if (checkController == CheckController.NONFINAL_REFS_AND_METHOD_CALLS
                        || checkController
                                == CheckController.NONFINAL_REFS_AND_METHOD_CALLS_METHOD) {
                    if (!((FlowExpressions.FieldAccess) r).getReceiver().toString().equals("this")
                            && !((FlowExpressions.FieldAccess) r).isFinal()) {
                        if (checkController == CheckController.NONFINAL_REFS_AND_METHOD_CALLS) {
                            return Result.failure(NO_REASSIGN_FIELD, anno.toString());
                        } else {
                            return Result.failure(SIDE_EFFECTING_METHOD, anno.toString());
                        }
                    } else {
                        FlowExpressions.Receiver oldR = r;
                        try {
                            r =
                                    factory.getReceiverFromJavaExpressionString(
                                            ((FlowExpressions.FieldAccess) r).getField().toString(),
                                            factory.getPath(n.getTree()));
                        } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {
                        }
                        if (oldR.equals(r)) {
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
            if (r instanceof FlowExpressions.MethodCall) {
                if (checkController == CheckController.NAME_AND_METHOD_CALLS
                        || checkController == CheckController.NONFINAL_REFS_AND_METHOD_CALLS) {
                    return Result.failure(NO_REASSIGN_FIELD_METHOD, anno.toString());
                }
                if (checkController == CheckController.NONFINAL_REFS_AND_METHOD_CALLS_METHOD) {
                    return Result.failure(SIDE_EFFECTING_METHOD, anno.toString());
                }
                for (FlowExpressions.Receiver param :
                        ((FlowExpressions.MethodCall) r).getParameters()) {
                    canonicalDependencies.add(param.toString());
                }
            }
        }
        if ((checkController == CheckController.NAME_AND_METHOD_CALLS
                        || checkController == CheckController.NAME_ONLY)
                && canonicalDependencies.contains(canonicalTargetName)) {
            return Result.failure(
                    NO_REASSIGN, canonicalTargetName, anno.toString(), canonicalTargetName);
        }
        return Result.SUCCESS;
    }

    void checkForRemainingAnnotations(
            List<AnnotatedTypeMirror> enclosedTypes,
            String canonicalTargetName,
            Node n,
            CheckController checkController) {
        for (AnnotatedTypeMirror atm : enclosedTypes) {
            for (AnnotationMirror anno : atm.getAnnotations()) {
                Result r = checkAnno(anno, canonicalTargetName, n, checkController);
                if (r.isFailure()) {
                    checker.report(r, n.getTree());
                }
            }
        }
    }

    void buildClearListForString(
            Map<? extends FlowExpressions.Receiver, CFValue> map,
            List<FlowExpressions.Receiver> toClear,
            String canonicalTargetName,
            Node n,
            CheckController checkController) {
        for (FlowExpressions.Receiver r : map.keySet()) {
            Set<AnnotationMirror> annos = map.get(r).getAnnotations();
            for (AnnotationMirror anno : annos) {
                if (checkAnno(anno, canonicalTargetName, n, checkController).isFailure()) {
                    toClear.add(r);
                }
            }
        }
    }

    void clearFromStore(String canonicalTargetName, Node n, CheckController checkController) {
        List<FlowExpressions.Receiver> toClear = new ArrayList<>();
        buildClearListForString(
                localVariableValues, toClear, canonicalTargetName, n, checkController);
        buildClearListForString(methodValues, toClear, canonicalTargetName, n, checkController);
        buildClearListForString(classValues, toClear, canonicalTargetName, n, checkController);
        buildClearListForString(fieldValues, toClear, canonicalTargetName, n, checkController);
        buildClearListForString(arrayValues, toClear, canonicalTargetName, n, checkController);
        for (FlowExpressions.Receiver r : toClear) {
            this.clearValue(r);
        }
    }
}
