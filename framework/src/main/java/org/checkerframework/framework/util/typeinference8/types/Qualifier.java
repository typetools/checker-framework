package org.checkerframework.framework.util.typeinference8.types;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.AnnotationUtils;

/** A wrapper around an {@link AnnotationMirror}. */
public class Qualifier extends AbstractQualifier {

  /** The annotation. */
  private final AnnotationMirror annotation;

  /** The name of the annotation. Cached because {@link #hashCode} is called often. */
  private final @CanonicalName String annotationName;

  /**
   * A wrapper around an {@link AnnotationMirror}.
   *
   * @param annotation the annotation
   * @param context the context
   */
  protected Qualifier(AnnotationMirror annotation, Java8InferenceContext context) {
    super(annotation, context);
    this.annotation = annotation;
    this.annotationName = AnnotationUtils.annotationName(annotation);
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
    return AnnotationUtils.areSame(annotation, ((Qualifier) o).annotation);
  }

  @Override
  public int hashCode() {
    // This ignores the annotation's element values, which `equals` does compare.  That is
    // permitted, just less discriminating.  Hashing the element values is not easy, because
    // `AnnotationUtils.areSame` treats an explicitly-written value as the same as a defaulted one.
    return annotationName.hashCode();
  }

  @Override
  public String toString() {
    return annotation.toString();
  }
}
