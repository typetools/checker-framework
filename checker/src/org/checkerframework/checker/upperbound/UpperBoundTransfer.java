package org.checkerframework.checker.upperbound;

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
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class UpperBoundTransfer extends CFAbstractTransfer<CFValue, CFStore, UpperBoundTransfer> {
    protected UpperBoundAnalysis analysis;

    private final AnnotationMirror LTL, EL, LTEL, UNKNOWN;

    private UpperBoundAnnotatedTypeFactory atypeFactory;

    public UpperBoundTransfer(UpperBoundAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        atypeFactory = (UpperBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        LTL = UpperBoundAnnotatedTypeFactory.LTL;
        EL = UpperBoundAnnotatedTypeFactory.EL;
        LTEL = UpperBoundAnnotatedTypeFactory.LTEL;
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

            // FIXME: the Index Checker includes this here. Not sure why - investigate.
            // if (dim instanceof NumericalAdditionNode) {
            //     if (isVarPlusOne((NumericalAdditionNode)dim, store, name)) {
            //         return result;
            //     }
            // }
            String[] names = {name};

            store.insertValue(
                    rec, UpperBoundAnnotatedTypeFactory.createLessThanLengthAnnotation(names));
        }
        return result;
    }

    // Make array.length have type EL(array).
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
    // refineGT and refineGTE.

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
         *  branch (i.e. when they are, actually, equal). In that
         *  case, we essentially want to refine them to the more
         *  precise of the two types, which we accomplish by refining
         *  each as if it were greater than or equal to the other.
         */
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);
        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNotEqual(
            NotEqualNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitNotEqual(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        /* != is equivalent to == and implemented the same way, but we
         * !have information about the else branch (i.e. when they are
         * !equal).
         */
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);
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
        if (leftType.hasAnnotationRelaxed(LTL)
                || leftType.hasAnnotationRelaxed(EL)
                || leftType.hasAnnotationRelaxed(LTEL)) {
            // Create an LTL for the right type.
            // There's a slight danger of losing information here but I'm going to do
            // it the simple-to-implement way for now and we can come back later FIXME.
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names = UpperBoundUtils.getValue(leftType.getAnnotationInHierarchy(LTEL));
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
        if (leftType.hasAnnotationRelaxed(LTL)) {
            // Create an LTL for the right type.
            // There's a slight danger of losing information here but I'm going to do
            // it the simple-to-implement way for now and we can come back later FIXME.
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names = UpperBoundUtils.getValue(leftType.getAnnotationInHierarchy(LTEL));
            store.insertValue(
                    rightRec, UpperBoundAnnotatedTypeFactory.createLessThanLengthAnnotation(names));
            return;
        } else if (leftType.hasAnnotationRelaxed(EL) || leftType.hasAnnotationRelaxed(LTEL)) {
            // Create an LTL for the right type.
            // There's a slight danger of losing information here but I'm going to do
            // it the simple-to-implement way for now and we can come back later FIXME.
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names = UpperBoundUtils.getValue(leftType.getAnnotationInHierarchy(LTEL));
            store.insertValue(
                    rightRec,
                    UpperBoundAnnotatedTypeFactory.createLessThanOrEqualToLengthAnnotation(names));
            return;
        }
    }
}
