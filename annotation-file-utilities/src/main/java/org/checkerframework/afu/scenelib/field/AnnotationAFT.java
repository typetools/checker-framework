package org.checkerframework.afu.scenelib.field;

import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.el.AnnotationDef;

/**
 * An {@link AnnotationAFT} represents a subannotation as the type of an annotation field and
 * contains the definition of the subannotation.
 */
public final class AnnotationAFT extends ScalarAFT {

  /** The definition of the subannotation. */
  public final AnnotationDef annotationDef;

  /** Constructs a new {@link AnnotationAFT} for a subannotation of the given definition. */
  public AnnotationAFT(AnnotationDef annotationDef) {
    this.annotationDef = annotationDef;
  }

  @Override
  public boolean isValidValue(Object o) {
    return o instanceof Annotation;
  }

  /**
   * The string representation of an {@link AnnotationAFT} looks like {@code @Foo} even though the
   * subannotation definition is logically part of the {@link AnnotationAFT}. This is because the
   * subannotation field type appears as {@code @Foo} in an index file and the subannotation
   * definition is written separately.
   */
  @Override
  public String toString() {
    return "annotation-field " + annotationDef.name;
  }

  @Override
  public void format(StringBuilder sb, Object o) {
    // Ensure the argument is an Annotation.
    Annotation anno = (Annotation) o;
    anno.toString(sb);
  }

  @Override
  public <R, T> R accept(AFTVisitor<R, T> v, T arg) {
    return v.visitAnnotationAFT(this, arg);
  }
}
