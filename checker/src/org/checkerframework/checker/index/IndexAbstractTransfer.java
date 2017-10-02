package org.checkerframework.checker.index;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFValue;

/**
 * This class provides methods shared by the Index Checker's internal checkers in their transfer
 * functions. In particular, it provides a common framework for visiting comparison operators.
 */
public abstract class IndexAbstractTransfer<
                Store extends CFAbstractStore<CFValue, Store>,
                Transfer extends CFAbstractTransfer<CFValue, Store, Transfer>>
        extends CFAbstractTransfer<CFValue, Store, Transfer> {

    public IndexAbstractTransfer(CFAbstractAnalysis<CFValue, Store, Transfer> analysis) {
        super(analysis);
    }

    @Override
    public TransferResult<CFValue, Store> visitGreaterThan(
            GreaterThanNode node, TransferInput<CFValue, Store> in) {
        TransferResult<CFValue, Store> result = super.visitGreaterThan(node, in);

        IndexRefinementInfo<Store> rfi = new IndexRefinementInfo<>(result, analysis, node);
        if (rfi.leftAnno == null || rfi.rightAnno == null) {
            return result;
        }
        // Refine the then branch.
        refineGT(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, rfi.thenStore, in);

        // Refine the else branch, which is the inverse of the then branch.
        refineGTE(
                /* left= */ rfi.right, /* leftAnno= */
                rfi.rightAnno,
                /* right= */ rfi.left, /* rightAnno= */
                rfi.leftAnno,
                rfi.elseStore,
                in);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, Store> visitGreaterThanOrEqual(
            GreaterThanOrEqualNode node, TransferInput<CFValue, Store> in) {
        TransferResult<CFValue, Store> result = super.visitGreaterThanOrEqual(node, in);

        IndexRefinementInfo<Store> rfi = new IndexRefinementInfo<>(result, analysis, node);
        if (rfi.leftAnno == null || rfi.rightAnno == null) {
            return result;
        }

        // Refine the then branch.
        refineGTE(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, rfi.thenStore, in);

        // Refine the else branch.
        refineGT(
                /* left= */ rfi.right, /* leftAnno= */
                rfi.rightAnno,
                /* right= */ rfi.left, /* rightAnno= */
                rfi.leftAnno,
                rfi.elseStore,
                in);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, Store> visitLessThanOrEqual(
            LessThanOrEqualNode node, TransferInput<CFValue, Store> in) {
        TransferResult<CFValue, Store> result = super.visitLessThanOrEqual(node, in);

        IndexRefinementInfo<Store> rfi = new IndexRefinementInfo<>(result, analysis, node);
        if (rfi.leftAnno == null || rfi.rightAnno == null) {
            return result;
        }

        // Refine the then branch. A <= is just a flipped >=.
        refineGTE(
                /* left= */ rfi.right, /* leftAnno= */
                rfi.rightAnno,
                /* right= */ rfi.left, /* rightAnno= */
                rfi.leftAnno,
                rfi.thenStore,
                in);

        // Refine the else branch.
        refineGT(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, rfi.elseStore, in);
        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, Store> visitLessThan(
            LessThanNode node, TransferInput<CFValue, Store> in) {
        TransferResult<CFValue, Store> result = super.visitLessThan(node, in);

        IndexRefinementInfo<Store> rfi = new IndexRefinementInfo<>(result, analysis, node);
        if (rfi.leftAnno == null || rfi.rightAnno == null) {
            return result;
        }

        // Refine the then branch. A < is just a flipped >.
        refineGT(
                /* left= */ rfi.right, /* leftAnno= */
                rfi.rightAnno,
                /* right= */ rfi.left, /* rightAnno= */
                rfi.leftAnno,
                rfi.thenStore,
                in);

        // Refine the else branch.
        refineGTE(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, rfi.elseStore, in);
        return rfi.newResult;
    }

    protected abstract void refineGT(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            Store store,
            TransferInput<CFValue, Store> in);

    protected abstract void refineGTE(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            Store store,
            TransferInput<CFValue, Store> in);
}
