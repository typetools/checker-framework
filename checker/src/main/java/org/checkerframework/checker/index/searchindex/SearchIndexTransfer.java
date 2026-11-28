package org.checkerframework.checker.index.searchindex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.checker.index.qual.NegativeIndexFor;
import org.checkerframework.checker.index.qual.SearchIndexFor;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * The transfer function for the SearchIndexFor checker. Allows {@link SearchIndexFor} to be refined
 * to {@link NegativeIndexFor}.
 *
 * <p>Contains 1 refinement rule: SearchIndexFor &rarr; NegativeIndexFor when compared against zero.
 */
public class SearchIndexTransfer extends IndexAbstractTransfer {

  /** The annotated type factory. */
  private final SearchIndexAnnotatedTypeFactory atypeFactory;

  /**
   * Create a new SearchIndexTransfer.
   *
   * @param analysis the CFAnalysis
   */
  public SearchIndexTransfer(CFAnalysis analysis) {
    super(analysis);
    atypeFactory = (SearchIndexAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  /**
   * If a {@link SearchIndexFor} value is negative, then refine it to {@link NegativeIndexFor}. This
   * method is called by refinement rules for binary comparison operators.
   *
   * <p>If the left value (in a greater-than or greater-than-or-equal binary comparison) is exactly
   * the value of {@code valueToCompareTo}, and the right side has type {@link SearchIndexFor}, then
   * the right side's new value in the store should become {@link NegativeIndexFor}.
   *
   * <p>For example, this allows the following code to typecheck:
   *
   * <pre>
   * <code>
   * {@literal @}SearchIndexFor("a") int index = Arrays.binarySearch(a, y);
   * if (index &lt; 0) {
   *   {@literal @}NegativeIndexFor("a") int negInsertionPoint = index;
   * }
   * </code>
   * </pre>
   *
   * @param valueToCompareTo this value must be 0 (for greater than or less than) or -1 (for greater
   *     than or equal or less than or equal)
   */
  private void refineSearchIndexToNegativeIndexFor(
      Node left, Node right, CFStore store, int valueToCompareTo) {
    assert valueToCompareTo == 0 || valueToCompareTo == -1;
    Long leftValue =
        ValueCheckerUtils.getExactValue(
            left.getTree(), atypeFactory.getValueAnnotatedTypeFactory());
    if (leftValue != null && leftValue == valueToCompareTo) {
      AnnotationMirror rightSIF =
          atypeFactory.getAnnotationMirror(right.getTree(), SearchIndexFor.class);
      if (rightSIF != null) {
        List<String> arrays =
            AnnotationUtils.getElementValueArray(
                rightSIF, atypeFactory.searchIndexForValueElement, String.class);
        // The SearchIndexFor annotation describes the arrays as they were when `right` was
        // evaluated. Omit each array that might change before the comparison completes, as `a` in
        // `(copy = index) < (a = new int[0]).length`.
        List<String> unchangedArrays = new ArrayList<>(arrays.size());
        for (String array : arrays) {
          if (!arrayChangedBeforeComparisonCompletes(array, right)) {
            unchangedArrays.add(array);
          }
        }
        if (unchangedArrays.isEmpty()) {
          return;
        }
        AnnotationMirror nif = atypeFactory.createNegativeIndexFor(unchangedArrays);
        if (unchangedArrays.size() == arrays.size()) {
          insertValueForNodesWithValueOf(store, right, nif);
        } else {
          replaceValueForComparedNode(store, right, nif);
        }
      }
    }
  }

  /**
   * Returns true if the given array might change after the given node is evaluated and before the
   * comparison that contains the node completes.
   *
   * @param array a Java expression string for an array, as in a {@link SearchIndexFor} annotation
   * @param node a node in an operand of a comparison
   * @return true if {@code array} might change before the comparison completes
   */
  private boolean arrayChangedBeforeComparisonCompletes(String array, Node node) {
    AnnotationMirror arrayNif =
        atypeFactory.createNegativeIndexFor(Collections.singletonList(array));
    return containsExpressionChangedBeforeComparisonCompletes(arrayNif, node);
  }

  /**
   * Refines the value in the store of each expression that has the value of the given node when the
   * comparison that contains the node completes, given that it is {@code nif}. The store's current
   * value for such an expression might name an array that changed before the comparison completed.
   * That value is incomparable to {@code nif}, so {@link CFStore#insertValue} would keep neither.
   * Instead, this method drops the changed arrays from the current value, and replaces the value by
   * the greatest lower bound of the result and {@code nif}.
   *
   * <p>For example, in {@code (copy = index) < zero(a = new int[0])}, the value of {@code copy} is
   * {@code @SearchIndexFor({"a", "b"})}, and {@code nif} is {@code @NegativeIndexFor("b")}. This
   * method replaces the value of {@code copy} by {@code @NegativeIndexFor("b")}.
   *
   * @param store the store to modify
   * @param node a node in an operand of a comparison
   * @param nif a {@link NegativeIndexFor} annotation that holds for {@code node} when the
   *     comparison completes
   */
  private void replaceValueForComparedNode(CFStore store, Node node, AnnotationMirror nif) {
    QualifierHierarchy hierarchy = atypeFactory.getQualifierHierarchy();
    for (Node internal : nodesWithValueOf(node)) {
      JavaExpression expr = JavaExpression.fromNode(internal);
      if (!CFAbstractStore.canInsertJavaExpression(expr)) {
        // No store holds a value for `expr`, such as a cast.
        continue;
      }
      CFValue oldValue = store.getValue(expr);
      AnnotationMirror oldAnno =
          oldValue == null
              ? null
              : hierarchy.findAnnotationInHierarchy(
                  oldValue.getAnnotations(), atypeFactory.UNKNOWN);
      AnnotationMirror newAnno = nif;
      if (oldAnno != null) {
        AnnotationMirror unchangedOldAnno = withoutChangedArrays(oldAnno, node);
        newAnno =
            hierarchy.greatestLowerBoundShallow(
                unchangedOldAnno, internal.getType(), nif, internal.getType());
      }
      store.replaceValue(expr, newAnno);
    }
  }

  /**
   * Returns the given annotation, without the arrays that might change after the given node is
   * evaluated and before the comparison that contains the node completes.
   *
   * @param anno a SearchIndex Checker qualifier
   * @param node a node in an operand of a comparison
   * @return {@code anno} without the arrays that might change before the comparison completes, or
   *     the top qualifier if that leaves no arrays in a {@link SearchIndexFor} or {@link
   *     NegativeIndexFor}
   */
  private AnnotationMirror withoutChangedArrays(AnnotationMirror anno, Node node) {
    boolean isNegativeIndexFor = atypeFactory.areSameByClass(anno, NegativeIndexFor.class);
    if (!isNegativeIndexFor && !atypeFactory.areSameByClass(anno, SearchIndexFor.class)) {
      return anno;
    }
    List<String> kept =
        new ArrayList<>(
            AnnotationUtils.getElementValueArray(
                anno,
                isNegativeIndexFor
                    ? atypeFactory.negativeIndexForValueElement
                    : atypeFactory.searchIndexForValueElement,
                String.class));
    kept.removeIf(array -> arrayChangedBeforeComparisonCompletes(array, node));
    if (kept.isEmpty()) {
      return atypeFactory.UNKNOWN;
    }
    return isNegativeIndexFor
        ? atypeFactory.createNegativeIndexFor(kept)
        : atypeFactory.createSearchIndexFor(kept);
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
