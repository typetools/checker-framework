package org.checkerframework.common.accumulation;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/**
 * This class only contains boilerplate code to permit AccumulationValue's accumulatedValues
 * functionality to interact with the rest of an accumulation type system.
 */
public class AccumulationAnalysis
    extends CFAbstractAnalysis<AccumulationValue, AccumulationStore, AccumulationTransfer> {

  /**
   * Constructs an AccumulationAnalysis.
   *
   * @param checker the checker
   * @param factory the type factory
   */
  public AccumulationAnalysis(BaseTypeChecker checker, AccumulationAnnotatedTypeFactory factory) {
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
