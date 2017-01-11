package org.checkerframework.checker.upperbound;

import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.checker.index.IndexRefinementInfo;
import org.checkerframework.checker.upperbound.qual.LTEqLengthOf;
import org.checkerframework.checker.upperbound.qual.LTLengthOf;
import org.checkerframework.checker.upperbound.qual.LTOMLengthOf;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;

public class UpperBoundTransfer extends IndexAbstractTransfer<UpperBoundStore, UpperBoundTransfer> {

    private UpperBoundAnnotatedTypeFactory atypeFactory;

    private QualifierHierarchy qualifierHierarchy;

    public UpperBoundTransfer(UpperBoundAnalysis analysis) {
        super(analysis);
        atypeFactory = (UpperBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        qualifierHierarchy = atypeFactory.getQualifierHierarchy();
    }

    // Refine the type of expressions used as an array dimension to be
    // less than length of the array to which the new array is
    // assigned.  For example int[] array = new int[expr]; the type of expr is @LTEqLength("array")
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
            Receiver dimRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), dim);

            Receiver arrayRec =
                    FlowExpressions.internalReprOf(analysis.getTypeFactory(), node.getTarget());

            Set<AnnotationMirror> oldType = in.getValueOfSubNode(dim).getAnnotations();

            AnnotationMirror newType =
                    qualifierHierarchy.greatestLowerBound(
                            qualifierHierarchy.findAnnotationInHierarchy(oldType, UNKNOWN),
                            atypeFactory.createLTEqLengthOfAnnotation(arrayRec.toString()));

            store.insertValue(dimRec, newType);
        }
        return result;
    }

    @Override
    protected TransferResult<CFValue, UpperBoundStore> strengthenAnnotationOfEqualTo(
            TransferResult<CFValue, UpperBoundStore> res,
            Node firstNode,
            Node secondNode,
            CFValue firstValue,
            CFValue secondValue,
            boolean notEqualTo) {
        TransferResult<CFValue, UpperBoundStore> result =
                super.strengthenAnnotationOfEqualTo(
                        res, firstNode, secondNode, firstValue, secondValue, notEqualTo);
        IndexRefinementInfo<UpperBoundStore> rfi =
                new IndexRefinementInfo<>(result, analysis, firstNode, secondNode);

        UpperBoundStore equalsStore = notEqualTo ? rfi.elseStore : rfi.thenStore;
        UpperBoundStore notEqualStore = notEqualTo ? rfi.thenStore : rfi.elseStore;

        refineEq(rfi.left, rfi.leftType, rfi.right, rfi.rightType, equalsStore);
        refineNeq(rfi.left, rfi.leftType, rfi.right, rfi.rightType, notEqualStore);

        return rfi.newResult;
    }

    /**
     * The implementation of the algorithm for refining a &gt; test. If an LTEL is greater than
     * something, then that thing must be an LTL. If an LTL is greater, than the other thing must be
     * LTOM.
     */
    protected void refineGT(
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
    protected void refineGTE(
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
