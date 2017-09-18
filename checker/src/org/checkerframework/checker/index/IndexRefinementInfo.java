package org.checkerframework.checker.index;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.BinaryOperationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.ErrorReporter;

/**
 * This struct contains all of the information that the refinement functions need. It's called by
 * each node function (i.e. greater than node, less than node, etc.) and then the results are passed
 * to the refinement function in whatever order is appropriate for that node. Its constructor
 * contains all of its logic.
 */
public class IndexRefinementInfo {

    public Node left, right;

    /**
     * Annotation for left and right expressions. Might be null if dataflow doesn't have a value for
     * the expression.
     */
    public AnnotationMirror leftAnno, rightAnno;

    public CFStore thenStore, elseStore;
    public ConditionalTransferResult<CFValue, CFStore> newResult;

    public IndexRefinementInfo(
            TransferResult<CFValue, CFStore> result,
            CFAbstractAnalysis<?, ?, ?> analysis,
            Node r,
            Node l) {
        right = r;
        left = l;

        if (analysis.getValue(right) == null || analysis.getValue(left) == null) {
            leftAnno = null;
            rightAnno = null;
            newResult =
                    new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
        } else {
            QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
            rightAnno = getAnno(analysis.getValue(right).getAnnotations(), hierarchy);
            leftAnno = getAnno(analysis.getValue(left).getAnnotations(), hierarchy);

            thenStore = result.getThenStore();
            elseStore = result.getElseStore();

            newResult =
                    new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
        }
    }

    public IndexRefinementInfo(
            TransferResult<CFValue, CFStore> result,
            CFAbstractAnalysis<?, ?, ?> analysis,
            BinaryOperationNode node) {
        this(result, analysis, node.getRightOperand(), node.getLeftOperand());
    }

    private static AnnotationMirror getAnno(
            Set<AnnotationMirror> set, QualifierHierarchy hierarchy) {
        if (set.size() == 1) {
            return set.iterator().next();
        }
        if (set.size() == 0) {
            return null;
        }
        Set<? extends AnnotationMirror> tops = hierarchy.getTopAnnotations();
        if (tops.size() != 1) {
            ErrorReporter.errorAbort(
                    IndexRefinementInfo.class
                            + ": Found multiple tops, but expected one. \nFound: %s",
                    tops.toString());
            return null; // dead code
        }
        return hierarchy.findAnnotationInSameHierarchy(set, tops.iterator().next());
    }
}
