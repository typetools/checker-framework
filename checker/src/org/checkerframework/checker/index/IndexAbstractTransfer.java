package org.checkerframework.checker.index;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;

public abstract class IndexAbstractTransfer extends CFTransfer {

    protected CFAnalysis analysis;

    protected IndexAbstractTransfer(CFAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThan(
            GreaterThanNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitGreaterThan(node, in);

        IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, node);

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

        IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, node);

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

        IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, node);

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

        IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, node);

        // Refine the then branch. A < is just a flipped >.
        refineGT(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);

        // Refine the else branch.
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
        return rfi.newResult;
    }

    protected abstract void refineGT(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            CFStore store);

    protected abstract void refineGTE(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            CFStore store);
}
