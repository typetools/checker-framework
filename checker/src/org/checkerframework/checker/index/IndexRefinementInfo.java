package org.checkerframework.checker.index;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.BinaryOperationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

/**
 * This struct contains all of the information that the refinement functions need. It's called by
 * each node function (i.e. greater than node, less than node, etc.) and then the results are passed
 * to the refinement function in whatever order is appropriate for that node. It's constructor
 * contains all of its logic.
 */
public class IndexRefinementInfo {

    public Node left, right;
    public Set<AnnotationMirror> leftType, rightType;
    public CFStore thenStore, elseStore;
    public ConditionalTransferResult<CFValue, CFStore> newResult;

    public IndexRefinementInfo(
            TransferResult<CFValue, CFStore> result, CFAnalysis analysis, Node r, Node l) {
        right = r;
        left = l;

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
            TransferResult<CFValue, CFStore> result,
            CFAnalysis analysis,
            BinaryOperationNode node) {
        this(result, analysis, node.getRightOperand(), node.getLeftOperand());
    }
}
