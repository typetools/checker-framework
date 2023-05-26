package org.checkerframework.common.accumulation;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationMirrorSet;

public class AccumulationAnalysis
    extends CFAbstractAnalysis<AccumulationValue, AccumulationStore, AccumulationTransfer> {
  protected AccumulationAnalysis(
      BaseTypeChecker checker,
      GenericAnnotatedTypeFactory<
              AccumulationValue,
              AccumulationStore,
              AccumulationTransfer,
              ? extends
                  CFAbstractAnalysis<AccumulationValue, AccumulationStore, AccumulationTransfer>>
          factory,
      int maxCountBeforeWidening) {
    super(checker, factory, maxCountBeforeWidening);
  }

  protected AccumulationAnalysis(
      BaseTypeChecker checker,
      GenericAnnotatedTypeFactory<
              AccumulationValue,
              AccumulationStore,
              AccumulationTransfer,
              ? extends
                  CFAbstractAnalysis<AccumulationValue, AccumulationStore, AccumulationTransfer>>
          factory) {
    super(checker, factory);
  }

  @Override
  public AccumulationStore createEmptyStore(boolean sequentialSemantics) {
    return new AccumulationStore(this, sequentialSemantics);
  }

  @Override
  public AccumulationStore createCopiedStore(AccumulationStore accumulationStore) {
    return new AccumulationStore(accumulationStore);
  }

  @Override
  public @Nullable AccumulationValue createAbstractValue(
      AnnotationMirrorSet annotations, TypeMirror underlyingType) {
    if (!CFAbstractValue.validateSet(annotations, underlyingType, atypeFactory)) {
      return null;
    }
    return new AccumulationValue(this, annotations, underlyingType);
  }
}
