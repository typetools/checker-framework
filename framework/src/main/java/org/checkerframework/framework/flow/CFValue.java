package org.checkerframework.framework.flow;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.javacutil.AnnotationMirrorSet;

// TODO: CFAbstractValue is also a set of annotations and a TypeMirror.
// This documentation does not clarify how this class is different.
/**
 * The default abstract value used in the Checker Framework: a set of annotations and a TypeMirror.
 *
 * <p>A subclass that adds state must override {@link #upperBoundOfEqualValuesIsThis} to return
 * false, unless {@link #equals} accounts for that state.
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
   * returns true. A subclass that adds state that {@code equals} ignores, and that its {@link
   * #upperBound(CFAbstractValue, TypeMirror, boolean)} combines, must override this method to
   * return false. Otherwise, the upper bound of two equal values would discard that state of the
   * argument.
   */
  @Override
  protected boolean upperBoundOfEqualValuesIsThis() {
    return true;
  }
}
