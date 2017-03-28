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
 * The transfer function for the SearchIndex checker. Allows {@link
 * org.checkerframework.checker.index.qual.SearchIndex SearchIndex} to become {@link
 * org.checkerframework.checker.index.qual.NegativeIndexFor NegativeIndexFor}.
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
     * Only possible refinements of {@link org.checkerframework.checker.index.qual.SearchIndex
     * SearchIndex} occur when a {@link org.checkerframework.checker.index.qual.SearchIndex
     * SearchIndex} is compared to these values.
     */
    private enum ValidComparisons {
        NEGATIVE_ONE,
        ZERO
    }

    /**
     * Special binary search handling: if the left value is exactly the value of toCompareTo, and
     * the right side is a {@link org.checkerframework.checker.index.qual.SearchIndex SearchIndex},
     * then the right side's new value in the store should be a new {@link
     * org.checkerframework.checker.index.qual.NegativeIndexFor NegativeIndexFor}.
     *
     * <p>For example, this allows the following code to typecheck:
     *
     * <pre>{@code
     * @SearchIndex("a") int x = Arrays.binarySearch(a, y);
     * if (x < 0) {
     *     @NegativeIndexFor("a") int z = x;
     * }
     *
     * }</pre>
     */
    private void specialHandlingForBinarySearch(
            Node left, Node right, CFStore store, ValidComparisons toCompareTo) {
        int valueToCompareTo;
        switch (toCompareTo) {
            case ZERO:
                valueToCompareTo = 0;
                break;
            case NEGATIVE_ONE:
                valueToCompareTo = -1;
                break;
            default:
                valueToCompareTo = 42;
                // Code issues a warning without valueToCompareTo getting a value here,
                // but this is dead code.
                assert false;
                break;
        }
        Long leftValue =
                IndexUtil.getExactValue(
                        left.getTree(), aTypeFactory.getValueAnnotatedTypeFactory());
        if (leftValue != null && leftValue == valueToCompareTo) {
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
        specialHandlingForBinarySearch(left, right, store, ValidComparisons.ZERO);
    }

    @Override
    protected void refineGTE(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {
        specialHandlingForBinarySearch(left, right, store, ValidComparisons.NEGATIVE_ONE);
    }
}
