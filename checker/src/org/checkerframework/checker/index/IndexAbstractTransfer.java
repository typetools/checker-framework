package org.checkerframework.checker.index;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFValue;

public abstract class IndexAbstractTransfer<
                IndexStore extends CFAbstractStore<CFValue, IndexStore>,
                MySelf extends IndexAbstractTransfer<IndexStore, MySelf>>
        extends CFAbstractTransfer<CFValue, IndexStore, MySelf> {

    protected IndexAbstractTransfer(CFAbstractAnalysis<CFValue, IndexStore, MySelf> analysis) {
        super(analysis);
    }

    @Override
    public TransferResult<CFValue, IndexStore> visitGreaterThan(
            GreaterThanNode node, TransferInput<CFValue, IndexStore> in) {
        TransferResult<CFValue, IndexStore> result = super.visitGreaterThan(node, in);

        IndexRefinementInfo<IndexStore> rfi = new IndexRefinementInfo<>(result, analysis, node);

        // Refine the then branch.
        refineGT(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        // Refine the else branch, which is the inverse of the then branch.
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, IndexStore> visitGreaterThanOrEqual(
            GreaterThanOrEqualNode node, TransferInput<CFValue, IndexStore> in) {
        TransferResult<CFValue, IndexStore> result = super.visitGreaterThanOrEqual(node, in);

        IndexRefinementInfo<IndexStore> rfi = new IndexRefinementInfo<>(result, analysis, node);

        // Refine the then branch.
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        // Refine the else branch.
        refineGT(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, IndexStore> visitLessThanOrEqual(
            LessThanOrEqualNode node, TransferInput<CFValue, IndexStore> in) {
        TransferResult<CFValue, IndexStore> result = super.visitLessThanOrEqual(node, in);

        IndexRefinementInfo<IndexStore> rfi = new IndexRefinementInfo<>(result, analysis, node);

        // Refine the then branch. A <= is just a flipped >=.
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);

        // Refine the else branch.
        refineGT(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, IndexStore> visitLessThan(
            LessThanNode node, TransferInput<CFValue, IndexStore> in) {
        TransferResult<CFValue, IndexStore> result = super.visitLessThan(node, in);

        IndexRefinementInfo<IndexStore> rfi = new IndexRefinementInfo<>(result, analysis, node);

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
            IndexStore store);

    protected abstract void refineGTE(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            IndexStore store);
}
