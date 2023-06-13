package org.checkerframework.checker.nullness;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/** Boilerplate code to glue together all the parts the KeyFor dataflow classes. */
public class KeyForAnalysis extends CFAbstractAnalysis<KeyForValue, KeyForStore, KeyForTransfer> {

  /**
   * Creates a new {@code KeyForAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  public KeyForAnalysis(BaseTypeChecker checker, KeyForAnnotatedTypeFactory factory) {
    super(checker, factory);
  }

  @Override
  public KeyForStore createEmptyStore(boolean sequentialSemantics) {
    return new KeyForStore(this, sequentialSemantics);
  }

  @Override
  public KeyForStore createCopiedStore(KeyForStore store) {
    return new KeyForStore(store);
  }

  @Override
  public KeyForValue createAbstractValue(
      AnnotationMirrorSet annotations, TypeMirror underlyingType) {

    if (!CFAbstractValue.validateSet(annotations, underlyingType, atypeFactory)) {
      return null;
    }
    return new KeyForValue(this, annotations, underlyingType);
  }
}
