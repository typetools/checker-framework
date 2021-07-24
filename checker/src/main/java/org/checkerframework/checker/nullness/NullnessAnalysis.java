package org.checkerframework.checker.nullness;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;

/**
 * The analysis class for the non-null type system (serves as factory for the transfer function,
 * stores and abstract values.
 */
public class NullnessAnalysis
    extends CFAbstractAnalysis<NullnessValue, NullnessStore, NullnessTransfer> {

  /**
   * Creates a new {@code NullnessAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  public NullnessAnalysis(BaseTypeChecker checker, NullnessAnnotatedTypeFactory factory) {
    super(checker, factory);
  }

  @Override
  public NullnessStore createEmptyStore(boolean sequentialSemantics) {
    return new NullnessStore(this, sequentialSemantics);
  }

  @Override
  public NullnessStore createCopiedStore(NullnessStore s) {
    return new NullnessStore(s);
  }

  @Override
  public NullnessValue createAbstractValue(
      Set<AnnotationMirror> annotations, TypeMirror underlyingType) {
    if (!CFAbstractValue.validateSet(annotations, underlyingType, qualifierHierarchy)) {
      return null;
    }
    return new NullnessValue(this, annotations, underlyingType);
  }
}
