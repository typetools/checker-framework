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
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;

/**
 * The RLCCalledMethods Store behaves like AccumulationStore but keeps every {@link
 * IteratedCollectionElement} in the store. When an assignment would remove one, such as {@code
 * list.get(i)} after {@code i++}, it is reset to having no called methods instead. This is needed
 * because {@link RLCCalledMethodsTransfer} only records calls on elements that are in the store.
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
   * Put back any iterated collection element that the assignment removed, with no called methods.
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
    for (IteratedCollectionElement element : elements) {
      if (!iteratedCollectionElements.containsKey(element)) {
        insertValue(element, top);
      }
    }
  }
}
