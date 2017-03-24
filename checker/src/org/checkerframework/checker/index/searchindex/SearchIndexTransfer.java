package org.checkerframework.checker.index.searchindex;

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.qual.SearchIndex;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

/**
 * The transfer function for the SearchIndex checker. Allows SearchIndex to become NegativeIndexFor.
 */
public class SearchIndexTransfer extends IndexAbstractTransfer {

    // The ATF (Annotated Type Factory).
    private SearchIndexAnnotatedTypeFactory aTypeFactory;

    private CFAnalysis analysis;

    public SearchIndexTransfer(CFAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        aTypeFactory = (SearchIndexAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    /**
     * Special binary search handling: if the left value is exactly comparison, and the right side
     * is a search index, then convert the right side in the store to a negative index for.
     */
    private void specialHandlingForBinarySearch(
            Node left, Node right, CFStore store, int comparison) {
        assert comparison == 0 || comparison == -1;
        Long leftValue =
                IndexUtil.getExactValue(
                        left.getTree(), aTypeFactory.getValueAnnotatedTypeFactory());
        if (leftValue != null && leftValue == comparison) {
            AnnotationMirror rightSI =
                    aTypeFactory.getAnnotationMirror(right.getTree(), SearchIndex.class);
            if (rightSI != null) {
                List<String> arrays = IndexUtil.getValueOfAnnotationWithStringArgument(rightSI);
                AnnotationMirror nif = aTypeFactory.createNegativeIndexFor(arrays);
                store.insertValue(
                        FlowExpressions.internalReprOf(analysis.getTypeFactory(), right), nif);
            }
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
        specialHandlingForBinarySearch(left, right, store, -1);
    }

    @Override
    protected void refineGTE(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {
        specialHandlingForBinarySearch(left, right, store, 0);
    }
}
