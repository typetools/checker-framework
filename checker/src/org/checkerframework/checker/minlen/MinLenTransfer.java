package org.checkerframework.checker.minlen;

import com.sun.source.tree.Tree;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.checker.index.IndexRefinementInfo;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;

public class MinLenTransfer extends IndexAbstractTransfer {

    protected CFAnalysis analysis;
    protected MinLenAnnotatedTypeFactory atypeFactory;

    private QualifierHierarchy qualifierHierarchy;

    public MinLenTransfer(CFAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        atypeFactory =
                (MinLenAnnotatedTypeFactory) (AnnotatedTypeFactory) analysis.getTypeFactory();
        qualifierHierarchy = atypeFactory.getQualifierHierarchy();
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
    public TransferResult<CFValue, CFStore> visitEqualTo(
            EqualToNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitEqualTo(node, in);

        IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, node);

        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        refineEq(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);

        // The else branch should only be refined if a length is being compared
        // to zero. The following code block implements this special case.
        // This special case occurs because zero is a hard bound on the bottom
        // of the array (i.e. no array can be smaller than zero), so in this
        // case the MinLen of the array is one.
        refineNotEqual(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);
        refineNotEqual(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNotEqual(
            NotEqualNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitNotEqual(node, in);

        IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, node);

        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);

        refineEq(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);

        // The then branch should only be refined if a length is being compared
        // to zero. The following code block implements this special case.
        // This special case occurs because zero is a hard bound on the bottom
        // of the array (i.e. no array can be smaller than zero), so in this
        // case the MinLen of the array is one.
        refineNotEqual(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);
        refineNotEqual(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        return rfi.newResult;
    }

    private void refineEq(
            Node left,
            Set<AnnotationMirror> leftTypeSet,
            Node right,
            Set<AnnotationMirror> rightTypeSet,
            CFStore store) {

        AnnotationMirror rightType =
                qualifierHierarchy.findAnnotationInHierarchy(rightTypeSet, atypeFactory.MIN_LEN_0);
        AnnotationMirror leftType =
                qualifierHierarchy.findAnnotationInHierarchy(leftTypeSet, atypeFactory.MIN_LEN_0);

        if (leftType == null || rightType == null) {
            return;
        }

        AnnotationMirror newType = qualifierHierarchy.greatestLowerBound(leftType, rightType);

        Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
        Receiver leftRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), left);

        store.insertValue(rightRec, newType);
        store.insertValue(leftRec, newType);
    }

    private Receiver getReceiverForFiNodeOrNull(Node node) {
        if (node instanceof FieldAccessNode) {
            Receiver rec =
                    FlowExpressions.internalReprOf(
                            analysis.getTypeFactory(), ((FieldAccessNode) node).getReceiver());
            return rec;
        }
        return null;
    }

    private Integer getNewMinLenForRefinement(
            Node fiNode, Node nonFiNode, Set<AnnotationMirror> leftType) {
        FieldAccessNode fi = null;
        Tree tree = null;
        Set<AnnotationMirror> type = null;
        // Only the length matters. This will miss an expression which
        // include an array length (like "a.length + 1"), but that's okay
        // for now.

        if (fiNode instanceof FieldAccessNode) {
            fi = (FieldAccessNode) fiNode;
            tree = nonFiNode.getTree();
            type = leftType;
        } else {
            return null;
        }

        if (fi == null || tree == null || type == null) {
            return null;
        }
        if (fi.getFieldName().equals("length")
                && fi.getReceiver().getType().getKind() == TypeKind.ARRAY) {
            // At this point, MinLen needs to invoke the constant value checker
            // to find out if it knows anything about what the length is being
            // compared to.

            AnnotatedTypeMirror valueType = atypeFactory.valueTypeFromTree(tree);

            if (valueType == null) {
                return null;
            }

            Integer newMinLen = atypeFactory.getMinLenFromValueType(valueType);

            return newMinLen;
        }
        return null;
    }

    private void refineNotEqual(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            CFStore store) {

        Receiver rec = getReceiverForFiNodeOrNull(left);
        Integer newMinLen = getNewMinLenForRefinement(left, right, leftType);

        if (newMinLen != null && newMinLen == 0 && rec != null) {
            store.insertValue(rec, atypeFactory.createMinLen(1));
        }
    }

    protected void refineGT(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            CFStore store) {

        Receiver rec = getReceiverForFiNodeOrNull(left);
        Integer newMinLen = getNewMinLenForRefinement(left, right, leftType);
        if (rec != null && newMinLen != null) {
            store.insertValue(rec, atypeFactory.createMinLen(newMinLen + 1));
        }
    }

    protected void refineGTE(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            CFStore store) {
        Receiver rec = getReceiverForFiNodeOrNull(left);
        Integer newMinLen = getNewMinLenForRefinement(left, right, leftType);
        if (rec != null && newMinLen != null) {
            store.insertValue(rec, atypeFactory.createMinLen(newMinLen));
        }
    }
}
