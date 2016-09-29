package org.checkerframework.checker.upperbound;

import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.upperbound.qual.*;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class UpperBoundTransfer extends CFTransfer {

    private final AnnotationMirror UNKNOWN;

    private UpperBoundAnnotatedTypeFactory atypeFactory;

    public UpperBoundTransfer(CFAnalysis analysis) {
        super(analysis);
        atypeFactory = (UpperBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        UNKNOWN = UpperBoundAnnotatedTypeFactory.UNKNOWN;
    }

    // Make variables used in array creation have reasonable types.
    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(
            AssignmentNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitAssignment(node, in);
        if (node.getExpression() instanceof ArrayCreationNode) {
            ArrayCreationNode acNode = (ArrayCreationNode) node.getExpression();
            CFStore store = result.getRegularStore();
            List<Node> nodeList = acNode.getDimensions();
            // The dimenions list is empty -> dimensions aren't known, I believe.
            if (nodeList.size() < 1) {
                return result;
            }
            Node dim = acNode.getDimension(0);
            Receiver rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), dim);
            String name = node.getTarget().toString();
            // This is silly, but...
            String[] names = {name};

            store.insertValue(
                    rec, UpperBoundAnnotatedTypeFactory.createLessThanLengthAnnotation(names));
        }
        return result;
    }

    /**
     *  Makes array.length have type EL(array).
     */
    @Override
    public TransferResult<CFValue, CFStore> visitFieldAccess(
            FieldAccessNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitFieldAccess(node, in);
        if (node.getFieldName().equals("length")) {
            // I'm concerned about the level of evil present in this code.
            // It's modeled on similar code in the old Index Checker, and it feels like a bad
            // way to do this, but I don't know a better way.
            String arrName = node.getReceiver().toString();
            AnnotationMirror anm =
                    UpperBoundAnnotatedTypeFactory.createEqualToLengthAnnotation(arrName);
            CFValue newResultValue =
                    analysis.createSingleAnnotationValue(
                            anm, result.getResultValue().getType().getUnderlyingType());
            return new RegularTransferResult<>(newResultValue, result.getRegularStore());
        }
        return result;
    }

    /**
     *  This struct contains all of the information that the refinement
     *  functions need. It's called by each node function (i.e. greater
     *  than node, less than node, etc.) and then the results are passed
     *  to the refinement function in whatever order is appropriate for
     *  that node. Its constructor contains all of its logic.
     *  I originally wrote this for LowerBoundTransfer but I'm duplicating it
     *  here since I need it again...maybe it should live elsewhere and be
     *  shared? I don't know where though.
     */
    private class RefinementInfo {
        public Node left, right;
        public AnnotatedTypeMirror leftType, rightType;
        public CFStore thenStore, elseStore;
        public ConditionalTransferResult<CFValue, CFStore> newResult;

        public RefinementInfo(
                TransferResult<CFValue, CFStore> result,
                TransferInput<CFValue, CFStore> in,
                Node r,
                Node l) {
            right = r;
            left = l;

            rightType = in.getValueOfSubNode(right).getType();
            leftType = in.getValueOfSubNode(left).getType();

            thenStore = result.getRegularStore();
            elseStore = thenStore.copy();

            newResult =
                    new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
        }
    }

    // So I actually just ended up copying these from Lower Bound Transfer too.
    // The only parts that are actually different are the definitions of
    // refineGT and refineGTE, and the handling of equals and not equals. The
    // code for the visitGreaterThan, visitLessThan, etc., are all identical to
    // their LBC counterparts.

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThan(
            GreaterThanNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitGreaterThan(node, in);
        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch.
        refineGT(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        // Refine the else branch, which is the inverse of the then branch.
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(
            GreaterThanOrEqualNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitGreaterThanOrEqual(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch.
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        // Refine the else branch.
        refineGT(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThanOrEqual(
            LessThanOrEqualNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitLessThanOrEqual(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch. A <= is just a flipped >=.
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);

        // Refine the else branch.
        refineGT(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThan(
            LessThanNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitLessThan(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch. A < is just a flipped >.
        refineGT(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);

        // Refine the else branch.
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitEqualTo(
            EqualToNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitEqualTo(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        /*  In an ==, we only can make conclusions about the then
         *  branch (i.e. when they are, actually, equal).
         */
        refineEq(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);
        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNotEqual(
            NotEqualNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitNotEqual(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        /* != is equivalent to == and implemented the same way, but we
         * only have information about the else branch (i.e. when they are
         * not equal).
         */
        refineEq(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
        return rfi.newResult;
    }

    /**
     * The implementation of the algorithm for refining a &gt; test.
     * If an EL, an LTL, or an LTEL is greater than something, then
     * that thing must be an LTL.
     */
    private void refineGT(
            Node left,
            AnnotatedTypeMirror leftType,
            Node right,
            AnnotatedTypeMirror rightType,
            CFStore store) {
        // First, check if the left type is one of the ones that tells us something.
        if (leftType.hasAnnotation(LessThanLength.class)
                || leftType.hasAnnotation(EqualToLength.class)
                || leftType.hasAnnotation(LessThanOrEqualToLength.class)) {
            // Create an LTL for the right type.
            // There's a slight danger of losing information here but I'm going to do
            // it the simple-to-implement way for now and we can come back later FIXME.
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names = UpperBoundUtils.getValue(leftType.getAnnotationInHierarchy(UNKNOWN));
            store.insertValue(
                    rightRec, UpperBoundAnnotatedTypeFactory.createLessThanLengthAnnotation(names));
            return;
        }
    }

    /**
     * If an LTL is greater than or equal to something, it must also be LTL.
     * If an EL or LTEL is greater than or equal to something, it must be be LTEL.
     */
    private void refineGTE(
            Node left,
            AnnotatedTypeMirror leftType,
            Node right,
            AnnotatedTypeMirror rightType,
            CFStore store) {
        if (leftType.hasAnnotation(LessThanLength.class)) {
            // Create an LTL for the right type.
            // There's a slight danger of losing information here but I'm going to do
            // it the simple-to-implement way for now and we can come back later FIXME.
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names = UpperBoundUtils.getValue(leftType.getAnnotationInHierarchy(UNKNOWN));
            store.insertValue(
                    rightRec, UpperBoundAnnotatedTypeFactory.createLessThanLengthAnnotation(names));
            return;
        } else if (leftType.hasAnnotation(EqualToLength.class)
                || leftType.hasAnnotation(LessThanOrEqualToLength.class)) {
            // Create an LTEL for the right type.
            // There's a slight danger of losing information here but I'm going to do
            // it the simple-to-implement way for now and we can come back later FIXME.
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names = UpperBoundUtils.getValue(leftType.getAnnotationInHierarchy(UNKNOWN));
            store.insertValue(
                    rightRec,
                    UpperBoundAnnotatedTypeFactory.createLessThanOrEqualToLengthAnnotation(names));
            return;
        }
    }

    private void refineEq(
            Node left,
            AnnotatedTypeMirror leftType,
            Node right,
            AnnotatedTypeMirror rightType,
            CFStore store) {
        // LTEL always implies that the other is LTEL.
        if (leftType.hasAnnotation(LessThanOrEqualToLength.class)) {
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names = UpperBoundUtils.getValue(leftType.getAnnotationInHierarchy(UNKNOWN));
            store.insertValue(
                    rightRec,
                    UpperBoundAnnotatedTypeFactory.createLessThanOrEqualToLengthAnnotation(names));
        }
        if (rightType.hasAnnotation(LessThanOrEqualToLength.class)) {
            Receiver leftRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), left);
            String[] names = UpperBoundUtils.getValue(rightType.getAnnotationInHierarchy(UNKNOWN));
            store.insertValue(
                    leftRec,
                    UpperBoundAnnotatedTypeFactory.createLessThanOrEqualToLengthAnnotation(names));
        }
        // Handles the case where either both are EL or one is EL and the other is Unknown.
        if (rightType.hasAnnotation(EqualToLength.class)
                && (!leftType.hasAnnotation(LessThanOrEqualToLength.class))
                && (!leftType.hasAnnotation(LessThanLength.class))) {
            Receiver leftRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), left);
            String[] names = UpperBoundUtils.getValue(rightType.getAnnotationInHierarchy(UNKNOWN));
            store.insertValue(
                    leftRec, UpperBoundAnnotatedTypeFactory.createEqualToLengthAnnotation(names));
        }

        if (leftType.hasAnnotation(EqualToLength.class)
                && (!rightType.hasAnnotation(LessThanOrEqualToLength.class))
                && (!rightType.hasAnnotation(LessThanLength.class))) {
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names = UpperBoundUtils.getValue(leftType.getAnnotationInHierarchy(UNKNOWN));
            store.insertValue(
                    rightRec, UpperBoundAnnotatedTypeFactory.createEqualToLengthAnnotation(names));
        }
        // An LTL and an EL mean that both are LTEL for the combined list of arrays.
        if ((leftType.hasAnnotation(EqualToLength.class)
                        && rightType.hasAnnotation(LessThanLength.class))
                || (leftType.hasAnnotation(LessThanLength.class)
                        && rightType.hasAnnotation(EqualToLength.class))) {
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            Receiver leftRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), left);
            String[] namesRight =
                    UpperBoundUtils.getValue(rightType.getAnnotationInHierarchy(UNKNOWN));
            String[] namesLeft =
                    UpperBoundUtils.getValue(leftType.getAnnotationInHierarchy(UNKNOWN));
            String[] names = concat(namesRight, namesLeft);
            store.insertValue(
                    rightRec,
                    UpperBoundAnnotatedTypeFactory.createLessThanOrEqualToLengthAnnotation(names));
            store.insertValue(
                    leftRec,
                    UpperBoundAnnotatedTypeFactory.createLessThanOrEqualToLengthAnnotation(names));
        }
        if (leftType.hasAnnotation(LessThanLength.class) && fOnlyUnknown(rightType)) {
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names = UpperBoundUtils.getValue(leftType.getAnnotationInHierarchy(UNKNOWN));
            store.insertValue(
                    rightRec, UpperBoundAnnotatedTypeFactory.createLessThanLengthAnnotation(names));
        }
        if (rightType.hasAnnotation(LessThanLength.class) && fOnlyUnknown(leftType)) {
            Receiver leftRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), left);
            String[] names = UpperBoundUtils.getValue(rightType.getAnnotationInHierarchy(UNKNOWN));
            store.insertValue(
                    leftRec, UpperBoundAnnotatedTypeFactory.createLessThanLengthAnnotation(names));
        }
    }

    private boolean fOnlyUnknown(AnnotatedTypeMirror type) {
        return (!type.hasAnnotation(LessThanLength.class)
                && !type.hasAnnotation(EqualToLength.class)
                && !type.hasAnnotation(LessThanOrEqualToLength.class));
    }

    // From: http://stackoverflow.com/questions/80476/how-can-i-concatenate-two-arrays-in-java
    // This just concatenates two generic arrays.
    private static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
