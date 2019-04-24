package org.checkerframework.checker.index.samelen;

import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * The transfer function for the SameLen checker. Contains three cases:
 *
 * <ul>
 *   <li>new array: "b = new T[a.length]" implies that b is the same length as a.
 *   <li>length equality: after "if (a.length == b.length)", a and b have the same length.
 *   <li>object equality: after "if (a == b)", a and b have the same length, if they are arrays or
 *       strings.
 * </ul>
 */
public class SameLenTransfer extends CFTransfer {

    private SameLenAnnotatedTypeFactory aTypeFactory;

    /** Shorthand for aTypeFactory.UNKNOWN. */
    private AnnotationMirror UNKNOWN;

    public SameLenTransfer(CFAnalysis analysis) {
        super(analysis);
        this.aTypeFactory = (SameLenAnnotatedTypeFactory) analysis.getTypeFactory();
        this.UNKNOWN = aTypeFactory.UNKNOWN;
    }

    /**
     * Gets the receiver sequence of a length access node, or null if {@code lengthNode} is not a
     * length access.
     */
    private Node getLengthReceiver(Node lengthNode) {
        if (isArrayLengthAccess(lengthNode)) {
            // lengthNode is a.length
            FieldAccessNode lengthFieldAccessNode = (FieldAccessNode) lengthNode;
            return lengthFieldAccessNode.getReceiver();
        } else if (aTypeFactory.getMethodIdentifier().isLengthOfMethodInvocation(lengthNode)) {
            // lengthNode is s.length()
            MethodInvocationNode lengthMethodInvocationNode = (MethodInvocationNode) lengthNode;
            return lengthMethodInvocationNode.getTarget().getReceiver();
        } else {
            return null;
        }
    }

    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(
            AssignmentNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitAssignment(node, in);

        // Handle b = new T[a.length]
        if (node.getExpression() instanceof ArrayCreationNode) {
            ArrayCreationNode acNode = (ArrayCreationNode) node.getExpression();
            if (acNode.getDimensions().size() == 1) {

                Node lengthNode = acNode.getDimension(0);
                Node lengthNodeReceiver = getLengthReceiver(lengthNode);

                if (lengthNodeReceiver != null) {
                    // "new T[a.length]" or "new T[s.length()]" is the right hand side of the
                    // assignment.  lengthNode is known to be "lengthNodeReceiver.length" or
                    // "lengthNodeReceiver.length()"

                    // targetRec is the receiver for the left hand side of the assignment.
                    Receiver targetRec =
                            FlowExpressions.internalReprOf(
                                    analysis.getTypeFactory(), node.getTarget());
                    Receiver otherRec =
                            FlowExpressions.internalReprOf(
                                    analysis.getTypeFactory(), lengthNodeReceiver);

                    AnnotationMirror lengthNodeAnnotation =
                            aTypeFactory
                                    .getAnnotatedType(lengthNodeReceiver.getTree())
                                    .getAnnotationInHierarchy(UNKNOWN);

                    AnnotationMirror combinedSameLen =
                            // In Java 9, this can be:
                            // aTypeFactory.createCombinedSameLen(
                            //         List.of(targetRec, otherRec), List.of(lengthNodeAnnotation));
                            aTypeFactory.createCombinedSameLen(
                                    targetRec, otherRec, UNKNOWN, lengthNodeAnnotation);

                    propagateCombinedSameLen(combinedSameLen, node, result.getRegularStore());
                    return result;
                }
            }
        }

        AnnotationMirror rightAnno =
                aTypeFactory
                        .getAnnotatedType(node.getExpression().getTree())
                        .getAnnotationInHierarchy(UNKNOWN);

        // If the left side of the assignment is an array or a string, then have both the right and
        // left side be SameLen of each other.

        Receiver targetRec =
                FlowExpressions.internalReprOf(analysis.getTypeFactory(), node.getTarget());

        Receiver exprRec =
                FlowExpressions.internalReprOf(analysis.getTypeFactory(), node.getExpression());

        if (IndexUtil.isSequenceType(node.getTarget().getType())
                || (rightAnno != null
                        && AnnotationUtils.areSameByClass(rightAnno, SameLen.class))) {

            AnnotationMirror rightAnnoOrUnknown = rightAnno == null ? UNKNOWN : rightAnno;

            AnnotationMirror combinedSameLen =
                    // In Java 9, this can be:
                    // aTypeFactory.createCombinedSameLen(
                    //         List.of(targetRec, exprRec), List.of(rightAnnoOrUnknown));
                    aTypeFactory.createCombinedSameLen(
                            targetRec, exprRec, UNKNOWN, rightAnnoOrUnknown);

            propagateCombinedSameLen(combinedSameLen, node, result.getRegularStore());
        }

        return result;
    }

    /**
     * Insert a @SameLen annotation into the store as the SameLen type of each array listed in it.
     *
     * @param sameLenAnno a {@code @SameLen} annotation. Not just an annotation in the SameLen
     *     hierarchy; this annotation must be {@code @SameLen(...)}.
     * @param node the node in the tree where the combination is happening. Used for context.
     * @param store the store to modify
     */
    private void propagateCombinedSameLen(AnnotationMirror sameLenAnno, Node node, CFStore store) {
        TreePath currentPath = aTypeFactory.getPath(node.getTree());
        if (currentPath == null) {
            return;
        }
        for (String expr : IndexUtil.getValueOfAnnotationWithStringArgument(sameLenAnno)) {
            Receiver recS;
            try {
                recS = aTypeFactory.getReceiverFromJavaExpressionString(expr, currentPath);
            } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {
                continue;
            }
            store.clearValue(recS);
            store.insertValue(recS, sameLenAnno);
        }
    }

    /** Returns true if node is of the form "someArray.length". */
    private boolean isArrayLengthAccess(Node node) {
        return (node instanceof FieldAccessNode
                && ((FieldAccessNode) node).getFieldName().equals("length")
                && ((FieldAccessNode) node).getReceiver().getType().getKind() == TypeKind.ARRAY);
    }

    /**
     * Handles refinement of equality comparisons. Assumes "a == b" or "a.length == b.length"
     * evaluates to true. The method gives a and b SameLen of each other in the store.
     */
    private void refineEq(Node left, Node right, CFStore store) {
        List<Receiver> receivers = new ArrayList<>();
        List<AnnotationMirror> annos = new ArrayList<>();
        for (Node internal : splitAssignments(left)) {
            receivers.add(FlowExpressions.internalReprOf(analysis.getTypeFactory(), internal));
            annos.add(getAnno(internal));
        }
        for (Node internal : splitAssignments(right)) {
            receivers.add(FlowExpressions.internalReprOf(analysis.getTypeFactory(), internal));
            annos.add(getAnno(internal));
        }

        AnnotationMirror combinedSameLen = aTypeFactory.createCombinedSameLen(receivers, annos);

        propagateCombinedSameLen(combinedSameLen, left, store);
    }

    /**
     * Return n's annotation from the SameLen hierarchy.
     *
     * <p>analysis.getValue fails if called on an lvalue. However, this method needs to always
     * succeed, even when n is an lvalue. Consider this code:
     *
     * <pre>{@code if ((a = b) == c) {...}}</pre>
     *
     * where a, b, and c are all arrays, and a has type {@code @SameLen("d")}. Afterwards, all three
     * should have the type {@code @SameLen({"a", "b", "c", "d"})}, but in order to accomplish this,
     * this method must return the type of a, which is an lvalue.
     */
    AnnotationMirror getAnno(Node n) {
        if (n.isLValue()) {
            return aTypeFactory.getAnnotatedType(n.getTree()).getAnnotationInHierarchy(UNKNOWN);
        }
        CFValue cfValue = analysis.getValue(n);
        if (cfValue == null) {
            return UNKNOWN;
        }
        return aTypeFactory
                .getQualifierHierarchy()
                .findAnnotationInHierarchy(cfValue.getAnnotations(), UNKNOWN);
    }

    /** Implements the transfer rules for both equal nodes and not-equals nodes. */
    @Override
    protected TransferResult<CFValue, CFStore> strengthenAnnotationOfEqualTo(
            TransferResult<CFValue, CFStore> result,
            Node firstNode,
            Node secondNode,
            CFValue firstValue,
            CFValue secondValue,
            boolean notEqualTo) {
        // If result is a Regular transfer, then the elseStore is a copy of the then store, that is
        // created when getElseStore is called.  So do that before refining any values.
        CFStore elseStore = result.getElseStore();
        CFStore thenStore = result.getThenStore();
        CFStore equalStore = notEqualTo ? elseStore : thenStore;

        Node firstLengthReceiver = getLengthReceiver(firstNode);
        Node secondLengthReceiver = getLengthReceiver(secondNode);

        if (firstLengthReceiver != null && secondLengthReceiver != null) {
            // Refinement in the else store if this is a.length == b.length (or length() in case of
            // strings).
            refineEq(firstLengthReceiver, secondLengthReceiver, equalStore);
        } else if (IndexUtil.isSequenceType(firstNode.getType())
                || IndexUtil.isSequenceType(secondNode.getType())) {
            refineEq(firstNode, secondNode, equalStore);
        }

        return new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
    }
}
