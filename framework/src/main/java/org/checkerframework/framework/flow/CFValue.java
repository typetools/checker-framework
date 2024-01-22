package org.checkerframework.framework.flow;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.javacutil.AnnotationMirrorSet;

// TODO: CFAbstractValue is also a set of annotations and a TypeMirror.
// This documentation does not clarify how this class is different.
/**
 * The default abstract value used in the Checker Framework: a set of annotations and a TypeMirror.
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
}
