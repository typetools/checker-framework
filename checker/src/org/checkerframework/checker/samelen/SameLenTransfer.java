package org.checkerframework.checker.samelen;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;

/**
 * The transfer function for the SameLen checker. Contains two interesting cases: 1. If an array is
 * created using another array's length for its length, then the arrays have the same length. 2. If
 * the lengths of two arrays are explicitly checked to be equal, they have the same length.
 */
public class SameLenTransfer extends CFTransfer {

    // The ATF (Annotated Type Factory).
    private SameLenAnnotatedTypeFactory aTypeFactory;

    /** Easy shorthand for SameLenUnknown.class, basically. */
    private AnnotationMirror UNKNOWN;

    private CFAnalysis analysis;

    public SameLenTransfer(CFAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        aTypeFactory = (SameLenAnnotatedTypeFactory) analysis.getTypeFactory();
        UNKNOWN = aTypeFactory.createSameLenUnknown();
    }

    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(
            AssignmentNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitAssignment(node, in);

        // Check if an array is being created.

        if (node.getExpression() instanceof ArrayCreationNode) {
            ArrayCreationNode acNode = (ArrayCreationNode) node.getExpression();
            if (acNode.getDimensions().size() == 1 && isArrayLengthAccess(acNode.getDimension(0))) {
                // This array that's being created is the same size as the other one.
                FieldAccessNode arrayLengthNode = (FieldAccessNode) acNode.getDimension(0);
                Node arrayLengthNodeReceiver = arrayLengthNode.getReceiver();
                AnnotationMirror arrayLengthAnnotation =
                        aTypeFactory
                                .getAnnotatedType(arrayLengthNodeReceiver.getTree())
                                .getAnnotationInHierarchy(UNKNOWN);
                AnnotationMirror combinedSameLen =
                        aTypeFactory.createCombinedSameLen(
                                arrayLengthNodeReceiver.toString(),
                                node.getTarget().toString(),
                                arrayLengthAnnotation,
                                UNKNOWN);

                Receiver targetRec =
                        FlowExpressions.internalReprOf(analysis.getTypeFactory(), node.getTarget());
                Receiver otherRec =
                        FlowExpressions.internalReprOf(
                                analysis.getTypeFactory(), arrayLengthNodeReceiver);

                result.getRegularStore().clearValue(targetRec);
                result.getRegularStore().insertValue(targetRec, combinedSameLen);
                result.getRegularStore().clearValue(otherRec);
                result.getRegularStore().insertValue(otherRec, combinedSameLen);
            }
        }

        return result;
    }

    private boolean isArrayLengthAccess(Node node) {
        return (node instanceof FieldAccessNode
                && ((FieldAccessNode) node).getFieldName().equals("length")
                && ((FieldAccessNode) node).getReceiver().getType().getKind() == TypeKind.ARRAY);
    }
    /**
     * Handles refinement of equality comparisons. Looks for a.length and b.length, then annotates
     * both a and b to have SameLen of each other in the store.
     */
    private void refineEq(Node left, Node right, CFStore store) {
        if (isArrayLengthAccess(left)) {
            if (isArrayLengthAccess(right)) {

                Node leftReceiverNode = ((FieldAccessNode) left).getReceiver();
                Node rightReceiverNode = ((FieldAccessNode) right).getReceiver();

                Receiver leftRec =
                        FlowExpressions.internalReprOf(analysis.getTypeFactory(), leftReceiverNode);
                Receiver rightRec =
                        FlowExpressions.internalReprOf(
                                analysis.getTypeFactory(), rightReceiverNode);

                AnnotationMirror rightReceiverAnno =
                        aTypeFactory
                                .getAnnotatedType(rightReceiverNode.getTree())
                                .getAnnotationInHierarchy(UNKNOWN);
                AnnotationMirror leftReceiverAnno =
                        aTypeFactory
                                .getAnnotatedType(leftReceiverNode.getTree())
                                .getAnnotationInHierarchy(UNKNOWN);
                AnnotationMirror combinedSameLen =
                        aTypeFactory.createCombinedSameLen(
                                rightReceiverNode.toString(),
                                leftReceiverNode.toString(),
                                rightReceiverAnno,
                                leftReceiverAnno);
                store.clearValue(leftRec);
                store.clearValue(rightRec);
                store.insertValue(leftRec, combinedSameLen);
                store.insertValue(rightRec, combinedSameLen);
            }
        }
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

        if (notEqualTo) {
            // Refinement in the else store if this is a.length != b.length.
            refineEq(firstNode, secondNode, result.getElseStore());
        } else {
            // Refinement in the then store if this is a.length == b.length.
            refineEq(firstNode, secondNode, result.getThenStore());
        }
        return result;
    }
}
