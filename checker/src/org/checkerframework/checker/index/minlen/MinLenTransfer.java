package org.checkerframework.checker.index.minlen;

import static org.checkerframework.checker.index.IndexUtil.getMinValue;

import java.util.Collections;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.checker.index.IndexRefinementInfo;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class MinLenTransfer extends IndexAbstractTransfer {

    protected MinLenAnnotatedTypeFactory atypeFactory;

    public MinLenTransfer(CFAnalysis analysis) {
        super(analysis);
        atypeFactory = (MinLenAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    @Override
    public TransferResult<CFValue, CFStore> visitArrayAccess(
            ArrayAccessNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitArrayAccess(node, in);
        AnnotatedTypeMirror valueType = atypeFactory.valueTypeFromTree(node.getArray().getTree());

        if (valueType.hasAnnotation(ArrayLen.class)) {
            // In this case, refine the MinLen to match the ArrayLen.
            AnnotationMirror arrayLenAnm = valueType.getAnnotation(ArrayLen.class);
            CFStore store = in.getRegularStore();
            int minlen = Collections.min(ValueAnnotatedTypeFactory.getArrayLength(arrayLenAnm));
            Receiver rec =
                    FlowExpressions.internalReprOf(analysis.getTypeFactory(), node.getArray());
            store.insertValue(rec, atypeFactory.createMinLen(minlen));
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
        if (rfi.leftAnno == null || rfi.rightAnno == null) {
            return result;
        }

        CFStore equalsStore = notEqualTo ? rfi.elseStore : rfi.thenStore;
        CFStore notEqualsStore = notEqualTo ? rfi.thenStore : rfi.elseStore;

        refineGTE(rfi.right, rfi.rightAnno, rfi.left, rfi.leftAnno, equalsStore, null);
        refineGTE(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, equalsStore, null);

        // Types in the not equal branch should only be refined if a length is being compared
        // to zero.
        // This special case occurs because zero is a hard bound on the bottom
        // of the array (i.e. no array can be smaller than zero), so in this
        // case the MinLen of the array is one.
        refineNotEqual(rfi.right, rfi.rightAnno, rfi.left, rfi.leftAnno, notEqualsStore);
        refineNotEqual(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, notEqualsStore);

        return rfi.newResult;
    }

    /**
     * Returns a receiver representing node if node is an FieldAccessNode. Otherwise returns null.
     */
    private Receiver getReceiverOfFieldAccessNode(Node node) {
        if (node instanceof FieldAccessNode) {
            Receiver rec =
                    FlowExpressions.internalReprOf(
                            analysis.getTypeFactory(), ((FieldAccessNode) node).getReceiver());
            return rec;
        }
        return null;
    }

    /**
     * Contains a special case that's only needed in the minlen hierarchy: if an array length is not
     * equal to zero, then the array must be at least {@code @MinLen(1)}.
     */
    private void refineNotEqual(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store) {

        Receiver rec = getReceiverOfFieldAccessNode(left);
        Long newMinLen = getMinValue(right.getTree(), atypeFactory.getValueAnnotatedTypeFactory());

        if (newMinLen != null && newMinLen == 0 && rec != null) {
            store.insertValue(rec, atypeFactory.createMinLen(1));
        }
    }

    @Override
    protected void refineGT(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {

        Receiver rec = getReceiverOfFieldAccessNode(left);
        Long newMinLen = getMinValue(right.getTree(), atypeFactory.getValueAnnotatedTypeFactory());
        if (rec != null && newMinLen != null) {
            store.insertValue(rec, atypeFactory.createMinLen(newMinLen.intValue() + 1));
        }
    }

    @Override
    protected void refineGTE(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {
        Receiver rec = getReceiverOfFieldAccessNode(left);
        Long newMinLen = getMinValue(right.getTree(), atypeFactory.getValueAnnotatedTypeFactory());
        if (rec != null && newMinLen != null) {
            store.insertValue(rec, atypeFactory.createMinLen(newMinLen.intValue()));
        }
    }
}
