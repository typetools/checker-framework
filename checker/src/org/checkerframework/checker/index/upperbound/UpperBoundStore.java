package org.checkerframework.checker.index.upperbound;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.MethodCall;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
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
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
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
    private SourceChecker checker;

    /**
     * This enum is used by {@link UpperBoundStore#checkAnno(AnnotationMirror, String, Node,
     * SideEffectKind)} to determine which properties of an annotation to check based on what kind
     * of side-effect has occurred.
     */
    private enum SideEffectKind {
        LOCAL_VAR_REASSIGNMENT,
        ARRAY_FIELD_REASSIGNMENT,
        NON_ARRAY_FIELD_REASSIGNMENT,
        SIDE_EFFECTING_METHOD_CALL
    }

    enum SideEffectError {
        NO_REASSIGN("reassignment.not.permitted"),
        NO_REASSIGN_FIELD("reassignment.field.not.permitted"),
        SIDE_EFFECTING_METHOD("side.effect.invalidation"),
        NO_REASSIGN_FIELD_METHOD("reassignment.field.not.permitted.method"),
        NO_ERROR("");

        String errorKey;

        SideEffectError(String errorKey) {
            this.errorKey = errorKey;
        }
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
            clearFromStore(null, n, SideEffectKind.SIDE_EFFECTING_METHOD_CALL);
            checkAnnotationsInClass(
                    enclosedTypes, null, n, SideEffectKind.SIDE_EFFECTING_METHOD_CALL);
        }
    }

    void checkAnnotationsInClass(
            List<AnnotatedTypeMirror> enclosedTypes,
            Receiver canonicalTargetName,
            Node n,
            SideEffectKind sideEffectKind) {
        for (AnnotatedTypeMirror atm : enclosedTypes) {
            List<Receiver> rs = getDependentReceivers(atm.getAnnotations(), n);
            for (Receiver r : rs) {
                SideEffectError result = isSideEffected(r, canonicalTargetName, sideEffectKind);
                if (result != SideEffectError.NO_ERROR) {
                    checker.report(Result.failure(result.errorKey, atm), n.getTree());
                }
            }
        }
    }

    @Override
    public void updateForAssignment(Node n, CFValue val) {
        // Do reassignment things here.
        super.updateForAssignment(n, val);

        // This code determines the list of dependences in types that are to be invalidated
        SideEffectKind sideEffectKind = null;

        if (n.getType().getKind() == TypeKind.ARRAY) {
            if (n instanceof LocalVariableNode) {
                // Do not warn about assigning to a final variable. javac handles this.
                if (!ElementUtils.isEffectivelyFinal(((LocalVariableNode) n).getElement())) {
                    sideEffectKind = SideEffectKind.LOCAL_VAR_REASSIGNMENT;
                }
            }
            if (n instanceof FieldAccessNode) {
                // Do not warn about assigning to a final field. javac handles this.
                if (!ElementUtils.isEffectivelyFinal(((FieldAccessNode) n).getElement())) {
                    sideEffectKind = SideEffectKind.ARRAY_FIELD_REASSIGNMENT;
                }
            }
        } else {
            if (n instanceof FieldAccessNode) {
                if (!n.getType().getKind().isPrimitive()) {
                    if (!ElementUtils.isEffectivelyFinal(((FieldAccessNode) n).getElement())) {
                        sideEffectKind = SideEffectKind.NON_ARRAY_FIELD_REASSIGNMENT;
                    }
                }
            }
        }

        // Find all possibly-invalidated types
        if (sideEffectKind != null) {
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
            clearFromStore(rec, n, sideEffectKind);
            checkAnnotationsInClass(enclosedTypes, rec, n, sideEffectKind);
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
     * <p>{@code reassignedVariableName} is the canonicalized (i.e. viewpoint-adapted, etc.) name of
     * the field being reassigned, if applicable.
     *
     * <p>Then, the appropriate checks are made based on the {@code sideEffect} the result is
     * returned.
     *
     * <p>There are three possible checks:
     *
     * <ul>
     *   <li>Does the annotation depend on the {@code reassignedVariableName} parameter?
     *   <li>Does the annotation depend on any non-final references?
     *   <li>Does the annotation include any method calls?
     * </ul>
     *
     * <p>The SideEffectKind is named based on the kind of side-effect that is occurring, which
     * determines which checks will occur. The SideEffectKind value {@link
     * SideEffectKind#LOCAL_VAR_REASSIGNMENT} means just the first check is performed. The value
     * {@link SideEffectKind#ARRAY_FIELD_REASSIGNMENT} indicates that the first and second checks
     * should be performed. Both {@link SideEffectKind#NON_ARRAY_FIELD_REASSIGNMENT} and {@link
     * SideEffectKind#SIDE_EFFECTING_METHOD_CALL} indicate that the second and third checks should
     * occur, but that different errors should be issued.
     */
    private List<Receiver> getDependentReceivers(AnnotationMirror anno, Node n) {
        UpperBoundAnnotatedTypeFactory factory =
                (UpperBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        if (!AnnotationUtils.hasElementValue(anno, "value")) {
            return Collections.emptyList();
        }
        List<Receiver> receivers = new ArrayList<>();
        UBQualifier.LessThanLengthOf qual =
                (UBQualifier.LessThanLengthOf) UBQualifier.createUBQualifier(anno);

        List<String> dependencies = new ArrayList<>();
        for (String s : qual.getArrays()) {
            dependencies.add(s);
        }
        dependencies.addAll(qual.getOffsetsAsStrings());

        for (String dependency : dependencies) {
            FlowExpressions.Receiver r;
            try {
                TreePath path = factory.getPath(n.getTree());
                if (path == null) {
                    continue;
                }
                r = factory.getReceiverFromJavaExpressionString(dependency, path);
            } catch (FlowExpressionParseException e) {
                continue;
            }
            if (r == null) {
                continue;
            }
            receivers.add(r);
        }

        return receivers;
    }

    private List<Receiver> getDependentReceivers(Set<AnnotationMirror> annos, Node n) {
        List<Receiver> receivers = new ArrayList<>();
        for (AnnotationMirror anno : annos) {
            receivers.addAll(getDependentReceivers(anno, n));
        }
        return receivers;
    }

    /**
     * Checks if the {@code receiver} may be effect by the side effect.
     *
     * @param receiver expression to check if the side effect effects it
     * @param reassignedVariable field that is reassigned as the side effect, possible null
     * @param sideEffectKind {@link SideEffectKind}
     * @param anno just for errors
     * @return non-null result
     */
    private static SideEffectError isSideEffected(
            Receiver receiver, Receiver reassignedVariable, SideEffectKind sideEffectKind) {
        if ((sideEffectKind == SideEffectKind.ARRAY_FIELD_REASSIGNMENT
                        || sideEffectKind == SideEffectKind.LOCAL_VAR_REASSIGNMENT)
                && receiver.containsSyntacticEqualReceiver(reassignedVariable)) {
            return SideEffectError.NO_REASSIGN;
        }

        // This while loops cycles through all of the fields being accessed and/or methods
        // called.
        // field1.method.field3...
        while (receiver != null) {
            if (receiver instanceof FieldAccess) {
                FieldAccess fieldAccess = (FieldAccess) receiver;
                if (!receiver.isUnmodifiableByOtherCode()) {
                    if (sideEffectKind == SideEffectKind.NON_ARRAY_FIELD_REASSIGNMENT) {
                        return SideEffectError.NO_REASSIGN_FIELD;
                    } else if (sideEffectKind == SideEffectKind.SIDE_EFFECTING_METHOD_CALL) {
                        return SideEffectError.SIDE_EFFECTING_METHOD;
                    }
                }
                receiver = fieldAccess.getReceiver();
            } else if (receiver instanceof MethodCall) {
                MethodCall methodCall = (MethodCall) receiver;
                switch (sideEffectKind) {
                    case ARRAY_FIELD_REASSIGNMENT:
                    case NON_ARRAY_FIELD_REASSIGNMENT:
                        return SideEffectError.NO_REASSIGN_FIELD_METHOD;
                    case SIDE_EFFECTING_METHOD_CALL:
                        return SideEffectError.SIDE_EFFECTING_METHOD;
                    default:
                        receiver = methodCall.getReceiver();
                }
            } else {
                break;
            }
        }
        return SideEffectError.NO_ERROR;
    }

    /**
     * Clears receivers from the store whose current value may be effected by the side effect.
     *
     * @param reassignedVar reassigned variable or null
     * @param n current node
     * @param sideEffect kind of side effect
     */
    private void clearFromStore(Receiver reassignedVar, Node n, SideEffectKind sideEffect) {
        Set<Entry<? extends Receiver, CFValue>> receiverAnnotationEntry = new HashSet<>();
        receiverAnnotationEntry.addAll(localVariableValues.entrySet());
        receiverAnnotationEntry.addAll(methodValues.entrySet());
        receiverAnnotationEntry.addAll(classValues.entrySet());
        receiverAnnotationEntry.addAll(fieldValues.entrySet());
        receiverAnnotationEntry.addAll(arrayValues.entrySet());

        for (Entry<? extends Receiver, CFValue> entry : receiverAnnotationEntry) {
            Receiver r = entry.getKey();
            for (Receiver dependent : getDependentReceivers(entry.getValue().getAnnotations(), n)) {
                if (isSideEffected(dependent, reassignedVar, sideEffect)
                        != SideEffectError.NO_ERROR) {
                    this.clearValue(r);
                }
            }
        }
    }
}
