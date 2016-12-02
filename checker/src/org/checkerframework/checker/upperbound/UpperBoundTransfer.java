package org.checkerframework.checker.upperbound;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.upperbound.qual.*;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;

public class UpperBoundTransfer extends CFTransfer {

    private final AnnotationMirror UNKNOWN;

    private UpperBoundAnnotatedTypeFactory atypeFactory;

    private QualifierHierarchy qualifierHierarchy;

    public UpperBoundTransfer(CFAnalysis analysis) {
        super(analysis);
        atypeFactory = (UpperBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        qualifierHierarchy = atypeFactory.getQualifierHierarchy();
        UNKNOWN = UpperBoundAnnotatedTypeFactory.UNKNOWN;
    }

    // Refine the type of expressions used as an array dimension to be
    // less than length of the array to which the new array is
    // assigned.
    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(
            AssignmentNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitAssignment(node, in);

        // When an existing array is assigned into, we need to blow up the store -
        // that is, we need to remove every instance of LTL and LTEL, since arrays
        // might be aliased, and when an array is modified to be a different length,
        // that could cause any of our information about arrays to be wrong.
        if (node.getTarget().getType().getKind() == TypeKind.ARRAY) {
            // This means that the existing store needs to be invalidated.
            // As far as I can tell the easiest way to do this is to just
            // create a new TransferResult.
            TransferResult<CFValue, CFStore> newResult =
                    new RegularTransferResult<CFValue, CFStore>(
                            result.getResultValue(), new CFStore(analysis, true));
            result = newResult;
        }

        // This handles when a new array is created.
        if (node.getExpression() instanceof ArrayCreationNode) {
            ArrayCreationNode acNode = (ArrayCreationNode) node.getExpression();
            CFStore store = result.getRegularStore();
            List<Node> nodeList = acNode.getDimensions();
            if (nodeList.size() < 1) {
                return result;
            }
            Node dim = acNode.getDimension(0);
            Receiver rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), dim);
            String name = node.getTarget().toString();
            String[] names = {name};

            store.insertValue(
                    rec, UpperBoundAnnotatedTypeFactory.createLTLengthOfAnnotation(names));
        }
        return result;
    }

    /** Makes array.length have type LTEL(array). */
    @Override
    public TransferResult<CFValue, CFStore> visitFieldAccess(
            FieldAccessNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitFieldAccess(node, in);
        if (node.getFieldName().equals("length")
                && node.getReceiver().getType().getKind() == TypeKind.ARRAY) {
            String arrName = node.getReceiver().toString();
            AnnotationMirror anm =
                    UpperBoundAnnotatedTypeFactory.createLTEqLengthOfAnnotation(arrName);
            CFValue newResultValue =
                    analysis.createSingleAnnotationValue(
                            anm, result.getResultValue().getUnderlyingType());
            return new RegularTransferResult<>(newResultValue, result.getRegularStore());
        }
        return result;
    }

    /**
     * This struct contains all of the information that the refinement functions need. It's called
     * by each node function (i.e. greater than node, less than node, etc.) and then the results are
     * passed to the refinement function in whatever order is appropriate for that node. Its
     * constructor contains all of its logic. I originally wrote this for LowerBoundTransfer but I'm
     * duplicating it here since I need it again...maybe it should live elsewhere and be shared? I
     * don't know where though.
     */
    private class RefinementInfo {
        public Node left, right;
        public Set<AnnotationMirror> leftType, rightType;
        public CFStore thenStore, elseStore;
        public ConditionalTransferResult<CFValue, CFStore> newResult;

        public RefinementInfo(
                TransferResult<CFValue, CFStore> result,
                TransferInput<CFValue, CFStore> in,
                Node r,
                Node l) {
            right = r;
            left = l;

            rightType = in.getValueOfSubNode(right).getAnnotations();
            leftType = in.getValueOfSubNode(left).getAnnotations();

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
     * The implementation of the algorithm for refining a &gt; test. If an EL, an LTL, or an LTEL is
     * greater than something, then that thing must be an LTL.
     */
    private void refineGT(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            CFStore store) {
        // First, check if the left type is one of the ones that tells us something.
        if (AnnotationUtils.containsSameByClass(leftType, LTLengthOf.class)
                || AnnotationUtils.containsSameByClass(leftType, LTEqLengthOf.class)) {
            // Create an LTL for the right type.

            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names =
                    UpperBoundUtils.getValue(
                            qualifierHierarchy.findAnnotationInHierarchy(leftType, UNKNOWN));

            AnnotationMirror newType =
                    qualifierHierarchy.greatestLowerBound(
                            qualifierHierarchy.findAnnotationInHierarchy(rightType, UNKNOWN),
                            UpperBoundAnnotatedTypeFactory.createLTLengthOfAnnotation(names));

            store.insertValue(rightRec, newType);
            return;
        }
    }

    /**
     * If an LTL is greater than or equal to something, it must also be LTL. If an EL or LTEL is
     * greater than or equal to something, it must be be LTEL.
     */
    private void refineGTE(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            CFStore store) {
        if (AnnotationUtils.containsSameByClass(leftType, LTLengthOf.class)) {
            // Create an LTL for the right type.
            // There's a slight danger of losing information here:
            // if the two annotations are LTL(a) and EL(b), for instance,
            // we lose some information.
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names =
                    UpperBoundUtils.getValue(
                            qualifierHierarchy.findAnnotationInHierarchy(leftType, UNKNOWN));

            AnnotationMirror newType =
                    qualifierHierarchy.greatestLowerBound(
                            qualifierHierarchy.findAnnotationInHierarchy(rightType, UNKNOWN),
                            UpperBoundAnnotatedTypeFactory.createLTLengthOfAnnotation(names));

            store.insertValue(rightRec, newType);
            return;
        } else if (AnnotationUtils.containsSameByClass(leftType, LTEqLengthOf.class)) {
            // Create an LTL for the right type.
            // There's a slight danger of losing information here:
            // if the two annotations are LTL(a) and EL(b), for instance,
            // we lose some information.
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names =
                    UpperBoundUtils.getValue(
                            qualifierHierarchy.findAnnotationInHierarchy(leftType, UNKNOWN));

            AnnotationMirror newType =
                    qualifierHierarchy.greatestLowerBound(
                            qualifierHierarchy.findAnnotationInHierarchy(rightType, UNKNOWN),
                            UpperBoundAnnotatedTypeFactory.createLTEqLengthOfAnnotation(names));

            store.insertValue(rightRec, newType);
            return;
        }
    }

    private void refineEq(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            CFStore store) {
        // LTEL always implies that the other is LTEL.
        if (AnnotationUtils.containsSameByClass(leftType, LTEqLengthOf.class)) {
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names =
                    UpperBoundUtils.getValue(
                            qualifierHierarchy.findAnnotationInHierarchy(leftType, UNKNOWN));

            AnnotationMirror newType =
                    qualifierHierarchy.greatestLowerBound(
                            qualifierHierarchy.findAnnotationInHierarchy(rightType, UNKNOWN),
                            UpperBoundAnnotatedTypeFactory.createLTEqLengthOfAnnotation(names));

            store.insertValue(rightRec, newType);
        }
        if (AnnotationUtils.containsSameByClass(rightType, LTEqLengthOf.class)) {
            Receiver leftRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), left);
            String[] names =
                    UpperBoundUtils.getValue(
                            qualifierHierarchy.findAnnotationInHierarchy(rightType, UNKNOWN));

            AnnotationMirror newType =
                    qualifierHierarchy.greatestLowerBound(
                            qualifierHierarchy.findAnnotationInHierarchy(leftType, UNKNOWN),
                            UpperBoundAnnotatedTypeFactory.createLTEqLengthOfAnnotation(names));

            store.insertValue(leftRec, newType);
        }
        if (AnnotationUtils.containsSameByClass(leftType, LTLengthOf.class)
                && fOnlyUnknown(rightType)) {
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names =
                    UpperBoundUtils.getValue(
                            qualifierHierarchy.findAnnotationInHierarchy(leftType, UNKNOWN));

            AnnotationMirror newType =
                    qualifierHierarchy.greatestLowerBound(
                            qualifierHierarchy.findAnnotationInHierarchy(rightType, UNKNOWN),
                            UpperBoundAnnotatedTypeFactory.createLTLengthOfAnnotation(names));

            store.insertValue(rightRec, newType);
        }
        if (AnnotationUtils.containsSameByClass(rightType, LTLengthOf.class)
                && fOnlyUnknown(leftType)) {
            Receiver leftRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), left);
            String[] names =
                    UpperBoundUtils.getValue(
                            qualifierHierarchy.findAnnotationInHierarchy(leftType, UNKNOWN));

            AnnotationMirror newType =
                    qualifierHierarchy.greatestLowerBound(
                            qualifierHierarchy.findAnnotationInHierarchy(leftType, UNKNOWN),
                            UpperBoundAnnotatedTypeFactory.createLTLengthOfAnnotation(names));

            store.insertValue(leftRec, newType);
        }
    }

    // This method really only exists because it's easier to leave it. It used
    // to serve an actual function.
    private boolean fOnlyUnknown(Set<AnnotationMirror> type) {
        return AnnotationUtils.containsSameByClass(type, UpperBoundUnknown.class);
    }

    // From: http://stackoverflow.com/questions/80476/how-can-i-concatenate-two-arrays-in-java
    // This just concatenates two generic arrays.
    private static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
