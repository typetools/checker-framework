package org.checkerframework.checker.index.upperbound;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.MethodCall;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.javacutil.AnnotationUtils;

public class UpperBoundUtil {

    /**
     * This enum is used by {@link #isSideEffected(Receiver, Receiver, SideEffectKind)} to determine
     * which properties of an annotation to check based on what kind of side-effect has occurred.
     */
    public enum SideEffectKind {
        LOCAL_VAR_REASSIGNMENT,
        ARRAY_FIELD_REASSIGNMENT,
        NON_ARRAY_FIELD_REASSIGNMENT,
        SIDE_EFFECTING_METHOD_CALL
    }

    public enum SideEffectError {
        NO_REASSIGN("reassignment.not.permitted"),
        NO_REASSIGN_FIELD("reassignment.field.not.permitted"),
        SIDE_EFFECTING_METHOD("side.effect.invalidation"),
        NO_REASSIGN_FIELD_METHOD("reassignment.field.not.permitted.method"),
        NO_ERROR("");

        /*@CompilerMessageKey*/ final String errorKey;

        /*@SuppressWarnings("assignment.type.incompatible")*/
        // suppressed because "" is not a key
        SideEffectError(String errorKey) {
            this.errorKey = errorKey;
        }
    }

    /**
     * Checks if the {@code receiver} may be affected by the side effect.
     *
     * @param receiver expression to check if the side effect affects it
     * @param reassignedVariable field that is reassigned as the side effect, possible null
     * @param sideEffectKind {@link SideEffectKind}
     * @return non-null result
     */
    public static SideEffectError isSideEffected(
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
    public static List<Receiver> getDependentReceivers(
            AnnotationMirror anno, TreePath path, UpperBoundAnnotatedTypeFactory factory) {
        if (!AnnotationUtils.hasElementValue(anno, "value")) {
            return Collections.emptyList();
        }
        List<Receiver> receivers = new ArrayList<>();
        UBQualifier.LessThanLengthOf qual =
                (UBQualifier.LessThanLengthOf) UBQualifier.createUBQualifier(anno);

        List<String> dependencies = new ArrayList<>();
        for (String s : qual.getSequences()) {
            dependencies.add(s);
        }
        dependencies.addAll(qual.getOffsetsAsStrings());

        for (String dependency : dependencies) {
            Receiver r;
            try {
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

    public static List<Receiver> getDependentReceivers(
            Set<AnnotationMirror> annos, TreePath path, UpperBoundAnnotatedTypeFactory factory) {
        List<Receiver> receivers = new ArrayList<>();
        for (AnnotationMirror anno : annos) {
            receivers.addAll(getDependentReceivers(anno, path, factory));
        }
        return receivers;
    }
}
