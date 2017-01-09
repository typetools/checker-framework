package org.checkerframework.checker.index;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.BinaryOperationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;

/**
 * This struct contains all of the information that the refinement functions need. It's called by
 * each node function (i.e. greater than node, less than node, etc.) and then the results are passed
 * to the refinement function in whatever order is appropriate for that node.
 */
public class IndexRefinementInfo<IndexStore extends Store<IndexStore>> {

    public Node left, right;
    public Set<AnnotationMirror> leftType, rightType;
    public IndexStore thenStore, elseStore;
    public ConditionalTransferResult<CFValue, IndexStore> newResult;

    public IndexRefinementInfo(
            TransferResult<CFValue, IndexStore> result,
            CFAbstractAnalysis<CFValue, ?, ?> analysis,
            Node right,
            Node left) {
        this.right = right;
        this.left = left;
        if (analysis.getValue(right) == null || analysis.getValue(left) == null) {
            leftType = null;
            rightType = null;
            newResult =
                    new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
        } else {

            rightType = analysis.getValue(right).getAnnotations();
            leftType = analysis.getValue(left).getAnnotations();

            thenStore = result.getThenStore();
            elseStore = result.getElseStore();

            newResult =
                    new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
        }
    }

    public IndexRefinementInfo(
            TransferResult<CFValue, IndexStore> result,
            CFAbstractAnalysis<CFValue, ?, ?> analysis,
            BinaryOperationNode node) {
        this(result, analysis, node.getRightOperand(), node.getLeftOperand());
    }
}
