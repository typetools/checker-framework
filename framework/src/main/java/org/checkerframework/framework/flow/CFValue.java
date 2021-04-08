package org.checkerframework.framework.flow;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;

/**
 * The default abstract value used in the Checker Framework: a set of annotations and a TypeMirror.
 */
public class CFValue extends CFAbstractValue<CFValue> {

  public CFValue(
      CFAbstractAnalysis<CFValue, ?, ?> analysis,
      Set<AnnotationMirror> annotations,
      TypeMirror underlyingType) {
    super(analysis, annotations, underlyingType);
  }
}
