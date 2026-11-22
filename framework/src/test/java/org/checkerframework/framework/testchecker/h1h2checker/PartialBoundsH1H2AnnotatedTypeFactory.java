package org.checkerframework.framework.testchecker.h1h2checker;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Top;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/**
 * The type factory for the {@link PartialBoundsH1H2Checker}. The default qualifier upper bound for
 * a type declaration is {@code @H1Top}, and there is none in the H2 hierarchy.
 */
public class PartialBoundsH1H2AnnotatedTypeFactory extends H1H2AnnotatedTypeFactory {

  /**
   * Creates a new PartialBoundsH1H2AnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  public PartialBoundsH1H2AnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  protected AnnotationMirrorSet getDefaultTypeDeclarationBounds() {
    return AnnotationMirrorSet.singleton(AnnotationBuilder.fromClass(elements, H1Top.class));
  }
}
