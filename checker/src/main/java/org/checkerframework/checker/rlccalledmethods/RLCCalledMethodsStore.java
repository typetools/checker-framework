package org.checkerframework.checker.rlccalledmethods;

import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.accumulation.AccumulationStore;
import org.checkerframework.common.accumulation.AccumulationValue;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.IteratedCollectionElement;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;

/**
 * The RLCCalledMethods Store behaves like AccumulationStore, except for an assignment that removes
 * an {@link IteratedCollectionElement}.
 *
 * <p>Usually, after such an assignment, the expression that names the element refers to the next
 * element of the collection, on which no methods have been called yet. For example, {@code i++}
 * makes {@code list.get(i)} refer to the next element. Likewise, the CFG desugars {@code for
 * (Resource r : list)} into a loop whose body starts with {@code r = it.next()}, which makes {@code
 * r} refer to the next element. So this store puts the element back with no called methods: {@link
 * RLCCalledMethodsTransfer} only records calls on elements that are in the store.
 *
 * <p>An assignment such as {@code r = other} in the loop body is different: after it, {@code r}
 * does not refer to an element of the collection, so a call on {@code r} must not count as a call
 * on the element. This store does not put the element back.
 */
public class RLCCalledMethodsStore extends AccumulationStore {

  /**
   * Creates an RLCCalledMethodsStore.
   *
   * @param analysis the analysis
   * @param sequentialSemantics if true, use sequential semantics; if false, use concurrent
   *     semantics
   */
  protected RLCCalledMethodsStore(
      CFAbstractAnalysis<AccumulationValue, AccumulationStore, ?> analysis,
      boolean sequentialSemantics) {
    super(analysis, sequentialSemantics);
  }

  /**
   * Creates an RLCCalledMethodsStore as a copy of the given store.
   *
   * @param other another store
   */
  protected RLCCalledMethodsStore(CFAbstractStore<AccumulationValue, AccumulationStore> other) {
    super(other);
  }

  /*
   * Put back any iterated collection element that the assignment removed, with no called methods,
   * unless the element is a variable that the loop body reassigned.
   */
  @Override
  public void updateForAssignment(Node n, @Nullable AccumulationValue val) {
    if (iteratedCollectionElements.isEmpty()) {
      super.updateForAssignment(n, val);
      return;
    }
    Set<IteratedCollectionElement> elements = new HashSet<>(iteratedCollectionElements.keySet());
    super.updateForAssignment(n, val);
    AnnotationMirror top = ((AccumulationAnnotatedTypeFactory) analysis.getTypeFactory()).top;
    JavaExpression target = JavaExpression.fromNode(n);
    for (IteratedCollectionElement element : elements) {
      @SuppressWarnings("interning:not.interned")
      boolean reassignsElementVariable = element.reference.equals(target) && element.node != n;
      if (!reassignsElementVariable && !iteratedCollectionElements.containsKey(element)) {
        insertValue(element, top);
      }
    }
  }
}
