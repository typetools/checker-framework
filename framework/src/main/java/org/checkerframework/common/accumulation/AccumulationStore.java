package org.checkerframework.common.accumulation;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;

/** This class is boilerplate, to enable the logic in AccumulationValue. */
public class AccumulationStore extends CFAbstractStore<AccumulationValue, AccumulationStore> {

  /**
   * Creates an AccumulationStore.
   *
   * @param analysis the analysis
   * @param sequentialSemantics if true, use sequential semantics; if false, use concurrent
   *     semantics
   */
  protected AccumulationStore(
      CFAbstractAnalysis<AccumulationValue, AccumulationStore, ?> analysis,
      boolean sequentialSemantics) {
    super(analysis, sequentialSemantics);
  }

  /**
   * Creates an AccumulationStore as a copy of the given store.
   *
   * @param other another store
   */
  protected AccumulationStore(CFAbstractStore<AccumulationValue, AccumulationStore> other) {
    super(other);
  }
}
