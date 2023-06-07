package org.checkerframework.common.accumulation;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;

/** This class is boilerplate, to enable the logic in AccumulationValue. */
public class AccumulationStore extends CFAbstractStore<AccumulationValue, AccumulationStore> {

  /**
   * Constructor matching super.
   *
   * @param analysis the analysis
   * @param sequentialSemantics whether to use sequential semantics (true) or concurrent semantics
   *     (false)
   */
  protected AccumulationStore(
      CFAbstractAnalysis<AccumulationValue, AccumulationStore, ?> analysis,
      boolean sequentialSemantics) {
    super(analysis, sequentialSemantics);
  }

  /**
   * Constructor matching super's copy constructor.
   *
   * @param other another store
   */
  protected AccumulationStore(CFAbstractStore<AccumulationValue, AccumulationStore> other) {
    super(other);
  }
}
