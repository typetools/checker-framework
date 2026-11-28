package org.checkerframework.framework.testchecker.h1h2checker;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Bot;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2Bot;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2Top;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/**
 * The type factory for the {@link PartialBoundsH1H2Checker}. The default qualifier upper bound for
 * a type declaration is {@code @H1S1}, and there is none in the H2 hierarchy. The default qualifier
 * in the H1 hierarchy is {@code @H1S1} rather than {@code @H1Top}, so that an unannotated use of a
 * type is valid.
 */
public class PartialBoundsH1H2AnnotatedTypeFactory extends H1H2AnnotatedTypeFactory {

  /** The default qualifier upper bounds for a type declaration: {@code @H1S1} alone. */
  private final AnnotationMirrorSet defaultTypeDeclarationBounds;

  /**
   * Creates a new PartialBoundsH1H2AnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  public PartialBoundsH1H2AnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    defaultTypeDeclarationBounds =
        AnnotationMirrorSet.singleton(AnnotationBuilder.fromClass(elements, H1S1.class));
  }

  @Override
  protected AnnotationMirrorSet getDefaultTypeDeclarationBounds() {
    return defaultTypeDeclarationBounds;
  }

  @Override
  protected void addCheckedCodeDefaults(QualifierDefaults defs) {
    defs.addCheckedCodeDefault(
        AnnotationBuilder.fromClass(elements, H1S1.class), TypeUseLocation.OTHERWISE);
    defs.addCheckedCodeDefault(
        AnnotationBuilder.fromClass(elements, H2Top.class), TypeUseLocation.OTHERWISE);
    defs.addCheckedCodeDefault(
        AnnotationBuilder.fromClass(elements, H1Bot.class), TypeUseLocation.LOWER_BOUND);
    defs.addCheckedCodeDefault(
        AnnotationBuilder.fromClass(elements, H2Bot.class), TypeUseLocation.LOWER_BOUND);
  }
}
