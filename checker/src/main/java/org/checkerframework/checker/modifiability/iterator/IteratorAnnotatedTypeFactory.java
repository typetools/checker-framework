package org.checkerframework.checker.modifiability.iterator;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.modifiability.ModifiabilityBaseAnnotatedTypeFactory;
import org.checkerframework.checker.modifiability.qual.IteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.MaybeIteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.PolyIteratorPolyMod;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;

/** The annotated type factory for the {@link IteratorChecker}. */
public class IteratorAnnotatedTypeFactory extends ModifiabilityBaseAnnotatedTypeFactory {

  /** The {@code @}{@link MaybeIteratorPolyMod} qualifier. */
  private final AnnotationMirror MAYBE_ITERATOR_POLY_MOD;

  /** The {@code @}{@link PolyIteratorPolyMod} qualifier. */
  private final AnnotationMirror POLY_ITERATOR_POLY_MOD;

  /**
   * Creates an IteratorAnnotatedTypeFactory.
   *
   * @param checker the associated type-checker
   */
  @SuppressWarnings("this-escape")
  public IteratorAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.MAYBE_ITERATOR_POLY_MOD =
        AnnotationBuilder.fromClass(elements, MaybeIteratorPolyMod.class);
    this.POLY_ITERATOR_POLY_MOD = AnnotationBuilder.fromClass(elements, PolyIteratorPolyMod.class);
    postInit();
  }

  @Override
  protected AnnotationMirror topAnnotation() {
    return MAYBE_ITERATOR_POLY_MOD;
  }

  @Override
  protected AnnotationMirror positiveCapability() {
    return ITERATOR_POLY_MOD;
  }

  @Override
  protected AnnotationMirror negativeCapability() {
    throw new UnsupportedOperationException("The iterator hierarchy has no negative qualifier.");
  }

  @Override
  protected boolean hasNegativeCapability() {
    return false;
  }

  @Override
  protected AnnotationMirror polyCapability() {
    return POLY_ITERATOR_POLY_MOD;
  }

  @Override
  protected boolean expandsModifiabilityAliases() {
    return false;
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(
            MaybeIteratorPolyMod.class, IteratorPolyMod.class, PolyIteratorPolyMod.class));
  }
}
