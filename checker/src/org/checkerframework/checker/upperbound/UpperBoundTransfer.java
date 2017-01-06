package org.checkerframework.checker.upperbound;

import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.upperbound.qual.LTEqLengthOf;
import org.checkerframework.checker.upperbound.qual.LTLengthOf;
import org.checkerframework.checker.upperbound.qual.LTOMLengthOf;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;

public class UpperBoundTransfer
        extends CFAbstractTransfer<CFValue, UpperBoundStore, UpperBoundTransfer> {

    private UpperBoundAnnotatedTypeFactory atypeFactory;

    private QualifierHierarchy qualifierHierarchy;

    public UpperBoundTransfer(UpperBoundAnalysis analysis) {
        super(analysis);
        atypeFactory = (UpperBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        qualifierHierarchy = atypeFactory.getQualifierHierarchy();
    }

    // Refine the type of expressions used as an array dimension to be
    // less than length of the array to which the new array is
    // assigned.
    @Override
    public TransferResult<CFValue, UpperBoundStore> visitAssignment(
            AssignmentNode node, TransferInput<CFValue, UpperBoundStore> in) {
        AnnotationMirror UNKNOWN = atypeFactory.UNKNOWN;
        TransferResult<CFValue, UpperBoundStore> result = super.visitAssignment(node, in);

        if (node.getExpression() instanceof ArrayCreationNode) {
            ArrayCreationNode acNode = (ArrayCreationNode) node.getExpression();
            UpperBoundStore store = result.getRegularStore();
            List<Node> nodeList = acNode.getDimensions();
            if (nodeList.size() < 1) {
                return result;
            }
            Node dim = acNode.getDimension(0);
            Receiver rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), dim);
            String name = node.getTarget().toString();
            String[] names = {name};

            Set<AnnotationMirror> oldType = in.getValueOfSubNode(dim).getAnnotations();

            AnnotationMirror newType =
                    qualifierHierarchy.greatestLowerBound(
                            qualifierHierarchy.findAnnotationInHierarchy(oldType, UNKNOWN),
                            atypeFactory.createLTLengthOfAnnotation(names));

            store.insertValue(rec, newType);
        }
        return result;
    }

    /**
     * This struct contains all of the information that the refinement functions need. It's called
     * by each node function (i.e. greater than node, less than node, etc.) and then the results are
     * passed to the refinement function in whatever order is appropriate for that node. Its
     * constructor contains all of its logic. Much of the code is duplicated or at least very
     * similar to the code in MinLenRefinementInfo and IndexRefinementInfo, but the same code can't
     * be reused because both the MLC and the UBC use their own stores.
     */
    private class UpperBoundRefinementInfo {
        public Node left, right;
        public Set<AnnotationMirror> leftType, rightType;
        public UpperBoundStore thenStore, elseStore;
        public ConditionalTransferResult<CFValue, UpperBoundStore> newResult;

        public UpperBoundRefinementInfo(
                TransferResult<CFValue, UpperBoundStore> result,
                TransferInput<CFValue, UpperBoundStore> in,
                BinaryOperationNode node) {
            right = node.getRightOperand();
            left = node.getLeftOperand();

            CFValue rightValue = in.getValueOfSubNode(right);

            if (rightValue != null) {
                rightType = rightValue.getAnnotations();
            } else {
                rightType = null;
            }

            CFValue leftValue = in.getValueOfSubNode(left);

            if (leftValue != null) {
                leftType = leftValue.getAnnotations();
            } else {
                leftType = null;
            }

            thenStore = result.getThenStore();
            elseStore = result.getElseStore();

            newResult =
                    new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
        }
    }

    // These are copied from Lower Bound Transfer.
    // The only parts that are actually different are the definitions of
    // refineGT and refineGTE, and the handling of equals and not equals. The
    // code for the visitGreaterThan, visitLessThan, etc., are all identical to
    // their LBC counterparts.

    @Override
    public TransferResult<CFValue, UpperBoundStore> visitGreaterThan(
            GreaterThanNode node, TransferInput<CFValue, UpperBoundStore> in) {
        TransferResult<CFValue, UpperBoundStore> result = super.visitGreaterThan(node, in);
        UpperBoundRefinementInfo rfi = new UpperBoundRefinementInfo(result, in, node);

        // Refine the then branch.
        refineGT(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        // Refine the else branch, which is the inverse of the then branch.
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, UpperBoundStore> visitGreaterThanOrEqual(
            GreaterThanOrEqualNode node, TransferInput<CFValue, UpperBoundStore> in) {
        TransferResult<CFValue, UpperBoundStore> result = super.visitGreaterThanOrEqual(node, in);

        UpperBoundRefinementInfo rfi = new UpperBoundRefinementInfo(result, in, node);

        // Refine the then branch.
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        // Refine the else branch.
        refineGT(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, UpperBoundStore> visitLessThanOrEqual(
            LessThanOrEqualNode node, TransferInput<CFValue, UpperBoundStore> in) {
        TransferResult<CFValue, UpperBoundStore> result = super.visitLessThanOrEqual(node, in);

        UpperBoundRefinementInfo rfi = new UpperBoundRefinementInfo(result, in, node);

        // Refine the then branch. A <= is just a flipped >=.
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);

        // Refine the else branch.
        refineGT(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, UpperBoundStore> visitLessThan(
            LessThanNode node, TransferInput<CFValue, UpperBoundStore> in) {
        TransferResult<CFValue, UpperBoundStore> result = super.visitLessThan(node, in);

        UpperBoundRefinementInfo rfi = new UpperBoundRefinementInfo(result, in, node);

        // Refine the then branch. A < is just a flipped >.
        refineGT(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);

        // Refine the else branch.
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, UpperBoundStore> visitEqualTo(
            EqualToNode node, TransferInput<CFValue, UpperBoundStore> in) {
        TransferResult<CFValue, UpperBoundStore> result = super.visitEqualTo(node, in);

        UpperBoundRefinementInfo rfi = new UpperBoundRefinementInfo(result, in, node);

        //  In an ==, only refine the then
        //  branch (i.e. when they are, actually, equal).
        refineEq(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        // With a few exceptions...
        refineNeq(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, UpperBoundStore> visitNotEqual(
            NotEqualNode node, TransferInput<CFValue, UpperBoundStore> in) {
        TransferResult<CFValue, UpperBoundStore> result = super.visitNotEqual(node, in);

        UpperBoundRefinementInfo rfi = new UpperBoundRefinementInfo(result, in, node);

        // != is equivalent to == and implemented the same way, but only the
        // else branch is refined.
        refineEq(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);

        // With a few exceptions...
        refineNeq(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        return rfi.newResult;
    }

    /**
     * The implementation of the algorithm for refining a &gt; test. If an LTEL is greater than
     * something, then that thing must be an LTL. If an LTL is greater, than the other thing must be
     * LTOM.
     */
    private void refineGT(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            UpperBoundStore store) {
        AnnotationMirror UNKNOWN = atypeFactory.UNKNOWN;

        // First, check if the left type is one of the ones that tells us something.
        if (AnnotationUtils.containsSameByClass(leftType, LTEqLengthOf.class)) {
            // Create an LTL for the right type.

            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names =
                    UpperBoundUtils.getValue(
                            qualifierHierarchy.findAnnotationInHierarchy(leftType, UNKNOWN));

            AnnotationMirror newType =
                    qualifierHierarchy.greatestLowerBound(
                            qualifierHierarchy.findAnnotationInHierarchy(rightType, UNKNOWN),
                            atypeFactory.createLTLengthOfAnnotation(names));

            store.insertValue(rightRec, newType);
            return;
        }
        if (AnnotationUtils.containsSameByClass(leftType, LTLengthOf.class)) {
            // Create an LTOM for the right type.

            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names =
                    UpperBoundUtils.getValue(
                            qualifierHierarchy.findAnnotationInHierarchy(leftType, UNKNOWN));

            AnnotationMirror newType =
                    qualifierHierarchy.greatestLowerBound(
                            qualifierHierarchy.findAnnotationInHierarchy(rightType, UNKNOWN),
                            atypeFactory.createLTOMLengthOfAnnotation(names));

            store.insertValue(rightRec, newType);
            return;
        }
    }

    /**
     * If an LTL is greater than or equal to something, it must also be LTL. If an LTEL is greater
     * than or equal to something, it must be be LTEL. If an LTOM is gte something, that's also
     * LTOM.
     */
    private void refineGTE(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            UpperBoundStore store) {
        AnnotationMirror UNKNOWN = atypeFactory.UNKNOWN;
        if (AnnotationUtils.containsSameByClass(leftType, LTLengthOf.class)) {
            // Create an LTL for the right type.
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names =
                    UpperBoundUtils.getValue(
                            qualifierHierarchy.findAnnotationInHierarchy(leftType, UNKNOWN));

            AnnotationMirror newType =
                    qualifierHierarchy.greatestLowerBound(
                            qualifierHierarchy.findAnnotationInHierarchy(rightType, UNKNOWN),
                            atypeFactory.createLTLengthOfAnnotation(names));

            store.insertValue(rightRec, newType);
            return;
        } else if (AnnotationUtils.containsSameByClass(leftType, LTEqLengthOf.class)) {
            // Create an LTL for the right type.

            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names =
                    UpperBoundUtils.getValue(
                            qualifierHierarchy.findAnnotationInHierarchy(leftType, UNKNOWN));

            AnnotationMirror newType =
                    qualifierHierarchy.greatestLowerBound(
                            qualifierHierarchy.findAnnotationInHierarchy(rightType, UNKNOWN),
                            atypeFactory.createLTEqLengthOfAnnotation(names));

            store.insertValue(rightRec, newType);
            return;
        } else if (AnnotationUtils.containsSameByClass(leftType, LTOMLengthOf.class)) {
            // Create an LTOM for the right type.

            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            String[] names =
                    UpperBoundUtils.getValue(
                            qualifierHierarchy.findAnnotationInHierarchy(leftType, UNKNOWN));

            AnnotationMirror newType =
                    qualifierHierarchy.greatestLowerBound(
                            qualifierHierarchy.findAnnotationInHierarchy(rightType, UNKNOWN),
                            atypeFactory.createLTOMLengthOfAnnotation(names));

            store.insertValue(rightRec, newType);
            return;
        }
    }

    private void refineEq(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            UpperBoundStore store) {
        AnnotationMirror UNKNOWN = atypeFactory.UNKNOWN;

        AnnotationMirror rightUpperboundType =
                qualifierHierarchy.findAnnotationInHierarchy(rightType, UNKNOWN);
        AnnotationMirror leftUpperboundType =
                qualifierHierarchy.findAnnotationInHierarchy(leftType, UNKNOWN);

        if (rightUpperboundType == null || leftUpperboundType == null) {
            return;
        }

        AnnotationMirror newType =
                qualifierHierarchy.greatestLowerBound(rightUpperboundType, leftUpperboundType);

        Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
        Receiver leftRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), left);

        store.insertValue(rightRec, newType);
        store.insertValue(leftRec, newType);
    }

    private boolean isArrayLengthFieldAccess(Node node) {
        if (!(node instanceof FieldAccessNode)) {
            return false;
        }
        FieldAccessNode fieldAccess = (FieldAccessNode) node;
        return fieldAccess.getFieldName().equals("length")
                && fieldAccess.getReceiver().getType().getKind() == TypeKind.ARRAY;
    }

    private void specialCaseForLTEL(
            Set<AnnotationMirror> leftType, Node left, Node right, UpperBoundStore store) {
        if (AnnotationUtils.containsSameByClass(leftType, LTEqLengthOf.class)) {
            if (isArrayLengthFieldAccess(right)) {
                FieldAccess fieldAccess =
                        FlowExpressions.internalReprOfFieldAccess(
                                atypeFactory, (FieldAccessNode) right);
                String arrayName = fieldAccess.getReceiver().toString();

                String[] names =
                        UpperBoundUtils.getValue(
                                AnnotationUtils.getAnnotationByClass(leftType, LTEqLengthOf.class));
                if (names.length != 1) {
                    // if there is more than one array, then no refinement takes place, because precise
                    // information is only available  about one array.
                    return;
                }

                if (names[0].equals(arrayName)) {
                    store.insertValue(
                            FlowExpressions.internalReprOf(analysis.getTypeFactory(), left),
                            atypeFactory.createLTLengthOfAnnotation(arrayName));
                }
            }
        }
    }

    private void specialCaseForLTL(
            Set<AnnotationMirror> leftType, Node left, Node right, UpperBoundStore store) {
        if (AnnotationUtils.containsSameByClass(leftType, LTLengthOf.class)) {
            if (isArrayLengthFieldAccess(right)) {
                FieldAccess fieldAccess =
                        FlowExpressions.internalReprOfFieldAccess(
                                atypeFactory, (FieldAccessNode) right);
                String arrayName = fieldAccess.getReceiver().toString();

                String[] names =
                        UpperBoundUtils.getValue(
                                AnnotationUtils.getAnnotationByClass(leftType, LTLengthOf.class));
                if (names.length != 1) {
                    // if there is more than one array, then no refinement takes place, because precise
                    // information is only available  about one array.
                    return;
                }
                if (names[0].equals(arrayName)) {
                    store.insertValue(
                            FlowExpressions.internalReprOf(analysis.getTypeFactory(), left),
                            atypeFactory.createLTOMLengthOfAnnotation(arrayName));
                }
            }
        }
    }

    private void refineNeq(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            UpperBoundStore store) {
        specialCaseForLTEL(leftType, left, right, store);
        specialCaseForLTEL(rightType, right, left, store);
        specialCaseForLTL(leftType, left, right, store);
        specialCaseForLTL(rightType, right, left, store);
    }
}
