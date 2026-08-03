package org.checkerframework.framework.util.typeinference8.types;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.AnnotationUtils;

/** A wrapper around an {@link AnnotationMirror}. */
public class Qualifier extends AbstractQualifier {

  /** The annotation. */
  private final AnnotationMirror annotation;

  /**
   * A wrapper around an {@link AnnotationMirror}.
   *
   * @param annotation the annotation
   * @param context the context
   */
  protected Qualifier(AnnotationMirror annotation, Java8InferenceContext context) {
    super(annotation, context);
    this.annotation = annotation;
  }

  /**
   * Returns the annotation.
   *
   * @return the annotation
   */
  public AnnotationMirror getAnnotation() {
    return annotation;
  }

  @Override
  public AnnotationMirror getInstantiation() {
    return annotation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Qualifier that = (Qualifier) o;

    return AnnotationUtils.areSame(annotation, that.annotation);
  }

  @Override
  public int hashCode() {
    // AnnotationMirror does not define hashCode, so hash the annotation's name. Annotations that
    // areSame() have the same name, so this is consistent with equals(), though it does not
    // distinguish annotations that differ only in their element values.
    return AnnotationUtils.annotationName(annotation).hashCode();
  }

  @Override
  public String toString() {
    return annotation.toString();
  }
}
