package org.checkerframework.checker.tainting;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/** Annotated type factory for the Tainting Checker. */
public class TaintingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link Untainted} annotation mirror. */
  private final AnnotationMirror UNTAINTED;

  /** A singleton set containing the {@code @}{@link Untainted} annotation mirror. */
  private final AnnotationMirrorSet setOfUntainted;

  /**
   * Creates a {@link TaintingAnnotatedTypeFactory}.
   *
   * @param checker the tainting checker
   */
  @SuppressWarnings("this-escape")
  public TaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.UNTAINTED = AnnotationBuilder.fromClass(getElementUtils(), Untainted.class);
    this.setOfUntainted = AnnotationMirrorSet.singleton(UNTAINTED);
    postInit();
  }

  @Override
  protected Set<AnnotationMirror> getEnumConstructorQualifiers() {
    return setOfUntainted;
  }
}
