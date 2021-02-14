package org.checkerframework.checker.index.searchindex;

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.checker.index.qual.NegativeIndexFor;
import org.checkerframework.checker.index.qual.SearchIndexFor;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

/**
 * The transfer function for the SearchIndexFor checker. Allows {@link SearchIndexFor} to be refined
 * to {@link NegativeIndexFor}.
 *
 * <p>Contains 1 refinement rule: SearchIndexFor &rarr; NegativeIndexFor when compared against zero.
 */
public class SearchIndexTransfer extends IndexAbstractTransfer {

    // The ATF (Annotated Type Factory).
    private SearchIndexAnnotatedTypeFactory aTypeFactory;

    public SearchIndexTransfer(CFAnalysis analysis) {
        super(analysis);
        aTypeFactory = (SearchIndexAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    /**
     * If a {@link SearchIndexFor} value is negative, then refine it to {@link NegativeIndexFor}.
     * This method is called by refinement rules for binary comparison operators.
     *
     * <p>If the left value (in a greater-than or greater-than-or-equal binary comparison) is
     * exactly the value of {@code valueToCompareTo}, and the right side has type {@link
     * SearchIndexFor}, then the right side's new value in the store should become {@link
     * NegativeIndexFor}.
     *
     * <p>For example, this allows the following code to typecheck:
     *
     * <pre>
     * <code>
     * {@literal @}SearchIndexFor("a") int index = Arrays.binarySearch(a, y);
     *  if (index &lt; 0) {
     *    {@literal @}NegativeIndexFor("a") int negInsertionPoint = index;
     *  }
     * </code>
     * </pre>
     *
     * @param valueToCompareTo this value must be 0 (for greater than or less than) or -1 (for
     *     greater than or equal or less than or equal)
     */
    private void refineSearchIndexToNegativeIndexFor(
            Node left, Node right, CFStore store, int valueToCompareTo) {
        assert valueToCompareTo == 0 || valueToCompareTo == -1;
        Long leftValue =
                ValueCheckerUtils.getExactValue(
                        left.getTree(), aTypeFactory.getValueAnnotatedTypeFactory());
        if (leftValue != null && leftValue == valueToCompareTo) {
            AnnotationMirror rightSI =
                    aTypeFactory.getAnnotationMirror(right.getTree(), SearchIndexFor.class);
            if (rightSI != null) {
                List<String> arrays =
                        ValueCheckerUtils.getValueOfAnnotationWithStringArgument(rightSI);
                AnnotationMirror nif = aTypeFactory.createNegativeIndexFor(arrays);
                store.insertValue(JavaExpression.fromNode(analysis.getTypeFactory(), right), nif);
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
        refineSearchIndexToNegativeIndexFor(left, right, store, 0);
    }

    @Override
    protected void refineGTE(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {
        refineSearchIndexToNegativeIndexFor(left, right, store, -1);
    }
}
