package org.checkerframework.checker.index.upperbound;

import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.MethodCall;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.javacutil.AnnotationUtils;

public class UpperBoundUtil {

    /**
     * This enum is used by {@link #isSideEffected(Receiver, Receiver, ReassignmentKind)} to
     * determine which properties of an annotation to check based on what kind of side-effect has
     * occurred.
     */
    public enum ReassignmentKind {
        LOCAL_VAR_REASSIGNMENT,
        ARRAY_FIELD_REASSIGNMENT,
        NON_ARRAY_FIELD_REASSIGNMENT,
        SIDE_EFFECTING_METHOD_CALL
    }

    public enum ReassignmentError {
        NO_REASSIGN("reassignment.not.permitted"),
        NO_REASSIGN_FIELD("reassignment.field.not.permitted"),
        SIDE_EFFECTING_METHOD("side.effect.invalidation"),
        NO_REASSIGN_FIELD_METHOD("reassignment.field.not.permitted.method"),
        NO_ERROR("");

        @CompilerMessageKey final String errorKey;

        @SuppressWarnings("assignment.type.incompatible")
        // suppressed because "" is not a key
        ReassignmentError(String errorKey) {
            this.errorKey = errorKey;
        }
    }

    /**
     * Checks if the {@code expression} may be affected by the side effect.
     *
     * <p>There are three possible checks:
     *
     * <ul>
     *   <li>Does the annotation depend on the {@code reassignedVariableName} parameter?
     *   <li>Does the annotation depend on any non-final references?
     *   <li>Does the annotation include any method calls?
     * </ul>
     *
     * <p>The ReassignmentKind is named based on the kind of side-effect that is occurring, which
     * determines which checks will occur. The ReassignmentKind value {@link
     * ReassignmentKind#LOCAL_VAR_REASSIGNMENT} means just the first check is performed. The value
     * {@link ReassignmentKind#ARRAY_FIELD_REASSIGNMENT} indicates that the first and second checks
     * should be performed. Both {@link ReassignmentKind#NON_ARRAY_FIELD_REASSIGNMENT} and {@link
     * ReassignmentKind#SIDE_EFFECTING_METHOD_CALL} indicate that the second and third checks should
     * occur, but that different errors should be issued.
     *
     * @param expression expression to check if the side effect effects it
     * @param reassignedVariable field that is reassigned as the side effect, possible null
     * @param reassignmentKind {@link ReassignmentKind}
     * @return non-null result
     */
    public static ReassignmentError isSideEffected(
            Receiver expression, Receiver reassignedVariable, ReassignmentKind reassignmentKind) {

        while (expression != null) {
            switch (reassignmentKind) {
                case LOCAL_VAR_REASSIGNMENT:
                    if (expression.containsSyntacticEqualReceiver(reassignedVariable)) {
                        return ReassignmentError.NO_REASSIGN;
                    }
                    return ReassignmentError.NO_ERROR;
                case ARRAY_FIELD_REASSIGNMENT:
                    if (expression.containsSyntacticEqualReceiver(reassignedVariable)) {
                        return ReassignmentError.NO_REASSIGN;
                    } else if (expression.containsOfClass(MethodCall.class)) {
                        return ReassignmentError.NO_REASSIGN_FIELD_METHOD;
                    } else {
                        break;
                    }
                case NON_ARRAY_FIELD_REASSIGNMENT:
                    if (expression.containsOfClass(MethodCall.class)) {
                        return ReassignmentError.NO_REASSIGN_FIELD_METHOD;
                    } else if (expression instanceof FieldAccess) {
                        if (!expression.isUnmodifiableByOtherCode()) {
                            return ReassignmentError.NO_REASSIGN_FIELD;
                        }
                    }
                    break;
                case SIDE_EFFECTING_METHOD_CALL:
                    if (expression.containsOfClass(MethodCall.class)) {
                        return ReassignmentError.SIDE_EFFECTING_METHOD;
                    } else if (expression instanceof FieldAccess) {
                        if (!expression.isUnmodifiableByOtherCode()) {
                            return ReassignmentError.SIDE_EFFECTING_METHOD;
                        }
                    }
                    break;
                default:
                    assert false : "Unexpected SideEffectKind";
                    return ReassignmentError.NO_ERROR;
            }

            // If this is a field access or method call, set expression to the receiver (i.e.
            // the previous object in the chain). Then loop around and process the receiver in
            // the same way. All calls/field accesses in the chain must be free of errors for
            // the whole chain to have no errors.
            if (expression.containsOfClass(MethodCall.class)) {
                expression = ((MethodCall) expression).getReceiver();
            } else if (expression instanceof FieldAccess) {
                expression = ((FieldAccess) expression).getReceiver();
            } else {
                // otherwise, there's nothing else to process. Return no_error.
                return ReassignmentError.NO_ERROR;
            }
        }

        // All elements in a chain have been successfully shown to have no error.
        return ReassignmentError.NO_ERROR;
    }

    /**
     * Returns a list of {@code Receiver}s that represent the program elements that
     *
     * @code{anno} depends on.
     * @param anno the annotation whose dependent program elements are returned
     * @param path the current path
     * @param factory the upperbound annotated type factory from which to read types
     * @return a list of program elements that {@code anno} depends on
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

    /**
     * Returns a list of all the program elements (as {@code Receiver}s) that the annotations in
     * {@code annos} depend on.
     *
     * @param annos a set of annotations
     * @param path the current path
     * @param factory an upperbound type factory from which to read types
     * @return a list of the program elements that the annotations in {@code annos} depend on
     */
    public static List<Receiver> getDependentReceivers(
            Set<AnnotationMirror> annos, TreePath path, UpperBoundAnnotatedTypeFactory factory) {
        List<Receiver> receivers = new ArrayList<>();
        for (AnnotationMirror anno : annos) {
            receivers.addAll(getDependentReceivers(anno, path, factory));
        }
        return receivers;
    }
}
