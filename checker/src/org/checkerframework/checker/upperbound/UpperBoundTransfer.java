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
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;

public class UpperBoundTransfer extends IndexAbstractTransfer {

    private UpperBoundAnnotatedTypeFactory atypeFactory;

    private QualifierHierarchy qualifierHierarchy;

    public UpperBoundTransfer(CFAnalysis analysis) {
        super(analysis);
        atypeFactory = (UpperBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        qualifierHierarchy = atypeFactory.getQualifierHierarchy();
    }

    private void combineFacts(
            CFStore store, Receiver receiver, AnnotationMirror oldAM, AnnotationMirror newAM) {
        AnnotationMirror combinedAM = atypeFactory.combineFacts(oldAM, newAM);
        // The old value is cleared from the store because it might be lower than
        // combinedAM.
        store.clearValue(receiver);
        store.insertValue(receiver, combinedAM);
    }

    // Refine the type of expressions used as an array dimension to be
    // less than length of the array to which the new array is
    // assigned.  For example int[] array = new int[expr]; the type of expr is @LTEqLength("array")
    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(
            AssignmentNode node, TransferInput<CFValue, CFStore> in) {
        AnnotationMirror UNKNOWN = atypeFactory.UNKNOWN;
        TransferResult<CFValue, CFStore> result = super.visitAssignment(node, in);

        if (node.getExpression() instanceof ArrayCreationNode) {
            ArrayCreationNode acNode = (ArrayCreationNode) node.getExpression();
            CFStore store = result.getRegularStore();
            List<Node> nodeList = acNode.getDimensions();
            if (nodeList.size() < 1) {
                return result;
            }
            Node dim = acNode.getDimension(0);
            Receiver dimRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), dim);

            Receiver arrayRec =
                    FlowExpressions.internalReprOf(analysis.getTypeFactory(), node.getTarget());

            Set<AnnotationMirror> oldType = in.getValueOfSubNode(dim).getAnnotations();
            AnnotationMirror oldAM = qualifierHierarchy.findAnnotationInHierarchy(oldType, UNKNOWN);
            AnnotationMirror newAM = atypeFactory.createLTEqLengthOfAnnotation(arrayRec.toString());
            combineFacts(store, dimRec, oldAM, newAM);
        }
        return result;
    }

    @Override
    protected TransferResult<CFValue, CFStore> strengthenAnnotationOfEqualTo(
            TransferResult<CFValue, CFStore> res,
            Node firstNode,
            Node secondNode,
            CFValue firstValue,
            CFValue secondValue,
            boolean notEqualTo) {
        TransferResult<CFValue, CFStore> result =
                super.strengthenAnnotationOfEqualTo(
                        res, firstNode, secondNode, firstValue, secondValue, notEqualTo);
        IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, firstNode, secondNode);

        CFStore equalsStore = notEqualTo ? rfi.elseStore : rfi.thenStore;
        CFStore notEqualStore = notEqualTo ? rfi.thenStore : rfi.elseStore;

        refineEq(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, equalsStore);
        refineNeq(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, notEqualStore);

        return rfi.newResult;
    }

    /**
     * The implementation of the algorithm for refining a &gt; test. If an LTEL is greater than
     * something, then that thing must be an LTL. If an LTL is greater, than the other thing must be
     * LTOM.
     */
    protected void refineGT(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store) {
        Class<?> nextHigherClass;
        if (AnnotationUtils.areSameByClass(leftAnno, LTEqLengthOf.class)) {
            nextHigherClass = LTLengthOf.class;
        } else if (AnnotationUtils.areSameByClass(leftAnno, LTLengthOf.class)) {
            nextHigherClass = LTOMLengthOf.class;
        } else {
            return;
        }
        Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
        String[] names = UpperBoundUtils.getValue(leftAnno);

        combineFacts(
                store, rightRec, rightAnno, atypeFactory.createAnnotation(nextHigherClass, names));
    }

    /**
     * If an LTL is greater than or equal to something, it must also be LTL. If an LTEL is greater
     * than or equal to something, it must be be LTEL. If an LTOM is gte something, that's also
     * LTOM.
     */
    protected void refineGTE(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store) {
        if (AnnotationUtils.areSameByClass(leftAnno, LTLengthOf.class)
                || AnnotationUtils.areSameByClass(leftAnno, LTEqLengthOf.class)
                || AnnotationUtils.areSameByClass(leftAnno, LTOMLengthOf.class)) {
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            combineFacts(store, rightRec, rightAnno, leftAnno);
        }
    }

    private void refineEq(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store) {
        if (rightAnno == null || leftAnno == null) {
            return;
        }

        Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
        Receiver leftRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), left);
        combineFacts(store, rightRec, rightAnno, leftAnno);
        combineFacts(store, leftRec, rightAnno, leftAnno);
    }

    private boolean isArrayLengthFieldAccess(Node node) {
        if (!(node instanceof FieldAccessNode)) {
            return false;
        }
        FieldAccessNode fieldAccess = (FieldAccessNode) node;
        return fieldAccess.getFieldName().equals("length")
                && fieldAccess.getReceiver().getType().getKind() == TypeKind.ARRAY;
    }

    private void refineNeq(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store) {
        refineNotEqualLtlandLteql(leftAnno, left, right, store);
        refineNotEqualLtlandLteql(rightAnno, right, left, store);
    }

    /**
     *
     *
     * <pre><code>
     * @LTEqLengthOf("array") int i;
     * if (i != array.length) {
     *     // refine the type of i to @LTLengthOf("array")
     * }
     * </code></pre>
     */
    private void refineNotEqualLtlandLteql(
            AnnotationMirror leftAnno, Node left, Node right, CFStore store) {
        Class<?> nextHigherClass;
        if (AnnotationUtils.areSameByClass(leftAnno, LTEqLengthOf.class)) {
            nextHigherClass = LTLengthOf.class;
        } else if (AnnotationUtils.areSameByClass(leftAnno, LTLengthOf.class)) {
            nextHigherClass = LTOMLengthOf.class;
        } else {
            return;
        }
        if (isArrayLengthFieldAccess(right)) {
            FieldAccess fieldAccess =
                    FlowExpressions.internalReprOfFieldAccess(
                            atypeFactory, (FieldAccessNode) right);
            String arrayName = fieldAccess.getReceiver().toString();

            String[] names = UpperBoundUtils.getValue(leftAnno);
            if (names.length != 1) {
                // if there is more than one array, then no refinement takes place, because precise
                // information is only available  about one array.
                return;
            }

            if (names[0].equals(arrayName)) {
                store.insertValue(
                        FlowExpressions.internalReprOf(analysis.getTypeFactory(), left),
                        atypeFactory.createAnnotation(nextHigherClass, arrayName));
            }
        }
    }
}
