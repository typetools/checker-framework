package org.checkerframework.common.accumulation;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;

public class AccumulationStore extends CFAbstractStore<AccumulationValue, AccumulationStore> {
  protected AccumulationStore(
      CFAbstractAnalysis<AccumulationValue, AccumulationStore, ?> analysis,
      boolean sequentialSemantics) {
    super(analysis, sequentialSemantics);
  }

  protected AccumulationStore(CFAbstractStore<AccumulationValue, AccumulationStore> other) {
    super(other);
  }
}
