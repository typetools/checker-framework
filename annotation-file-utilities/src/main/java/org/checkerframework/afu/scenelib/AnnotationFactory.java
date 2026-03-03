package org.checkerframework.afu.scenelib;

import java.util.Map;
import java.util.Set;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.checker.signature.qual.BinaryName;

/**
 * A very simple {@link AnnotationFactory AnnotationFactory} that creates {@link Annotation}s. It is
 * interested in all annotations and determines their definitions automatically from the fields
 * supplied. Use the singleton {@link #saf}.
 */
public final class AnnotationFactory {
  private AnnotationFactory() {}

  /** The singleton {@link AnnotationFactory}. */
  public static final AnnotationFactory saf = new AnnotationFactory();

  /**
   * Returns an {@link AnnotationBuilder} appropriate for building an {@link Annotation} of the
   * given type name.
   *
   * @param def the definition for the annotation to be built
   */
  public AnnotationBuilder beginAnnotation(AnnotationDef def, String source) {
    return new AnnotationBuilder(def, source);
  }

  /**
   * Returns an {@link AnnotationBuilder}. Tries to look up the AnnotationDef in adefs; if not
   * found, inserts in adefs.
   */
  public AnnotationBuilder beginAnnotation(
      java.lang.annotation.Annotation a, Map<String, AnnotationDef> adefs) {
    AnnotationDef def = AnnotationDef.fromClass(a.annotationType(), adefs);
    return new AnnotationBuilder(def, "Annotation " + a.annotationType());
  }

  /**
   * Returns an {@link AnnotationBuilder} appropriate for building an {@link Annotation} of the
   * given type name.
   *
   * @param typeName the name of the annotation being built
   * @param tlAnnotationsHere the top-level meta-annotations on the annotation being built
   * @param source where the annotation came from, such as a filename
   * @return an {@link AnnotationBuilder} appropriate for building a {@link Annotation} of the given
   *     type name
   */
  public AnnotationBuilder beginAnnotation(
      @BinaryName String typeName, Set<Annotation> tlAnnotationsHere, String source) {
    assert typeName != null;
    return new AnnotationBuilder(typeName, tlAnnotationsHere, source);
  }
}
