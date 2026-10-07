package org.checkerframework.framework.flow;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.javacutil.AnnotationMirrorSet;

// TODO: CFAbstractValue is also a set of annotations and a TypeMirror.
// This documentation does not clarify how this class is different.
/**
 * The default abstract value used in the Checker Framework: a set of annotations and a TypeMirror.
 *
 * <p>A subclass of CFValue gets a faster upper-bound computation only if it overrides {@link
 * #upperBoundOfEqualValuesIsThis} to return true, which is correct if {@link #equals} accounts for
 * all of the subclass's state.
 */
public class CFValue extends CFAbstractValue<CFValue> {

  /**
   * Creates a new CFValue.
   *
   * @param analysis the analysis
   * @param annotations the annotations
   * @param underlyingType the underlying type
   */
  public CFValue(
      CFAbstractAnalysis<CFValue, ?, ?> analysis,
      AnnotationMirrorSet annotations,
      TypeMirror underlyingType) {
    super(analysis, annotations, underlyingType);
  }

  /**
   * {@inheritDoc}
   *
   * <p>A CFValue has no state beyond what {@link #equals} accounts for, so this implementation
   * returns true for a CFValue. It returns false for an instance of a subclass, which might add
   * state that {@code equals} ignores but that its {@link #upperBound(CFAbstractValue, TypeMirror,
   * boolean)} combines; the upper bound of two equal values would then discard that state of the
   * argument.
   */
  @Override
  protected boolean upperBoundOfEqualValuesIsThis() {
    return getClass() == CFValue.class;
  }
}
