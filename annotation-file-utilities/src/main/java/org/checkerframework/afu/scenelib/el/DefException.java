package org.checkerframework.afu.scenelib.el;

/**
 * Thrown by {@link DefCollector} if the scene contains two different definitions of the same
 * annotation type that cannot be {@linkplain AnnotationDef#unify unified}.
 */
public class DefException extends Exception {
  private static final long serialVersionUID = 1152640422L;

  /** The name of the annotation type that had two conflicting definitions. */
  public final String annotationType;

  DefException(String annotationType, AnnotationDef def1, AnnotationDef def2) {
    super(
        "Conflicting definitions of annotation type "
            + annotationType
            + "\n  "
            + def1
            + "\n    "
            + def1.source
            + "\n  "
            + def2
            + "\n    "
            + def2.source);
    this.annotationType = annotationType;
  }
}
