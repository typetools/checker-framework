package org.checkerframework.checker.modifiability.shrink;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.modifiability.ModifiabilityBaseAnnotatedTypeFactory;
import org.checkerframework.checker.modifiability.qual.BottomShrinkable;
import org.checkerframework.checker.modifiability.qual.MaybeShrinkable;
import org.checkerframework.checker.modifiability.qual.PolyShrinkable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;
import org.checkerframework.checker.modifiability.qual.Unshrinkable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TypesUtils;

/** The annotated type factory for the {@link ShrinkChecker}. */
public class ShrinkAnnotatedTypeFactory extends ModifiabilityBaseAnnotatedTypeFactory {

  /** The {@code @}{@link MaybeShrinkable} qualifier. */
  private final AnnotationMirror MAYBE_SHRINKABLE;

  /** The {@code @}{@link Shrinkable} qualifier. */
  private final AnnotationMirror SHRINKABLE;

  /** The {@code @}{@link Unshrinkable} qualifier. */
  private final AnnotationMirror UNSHRINKABLE;

  /** The {@code @}{@link PolyShrinkable} qualifier. */
  private final AnnotationMirror POLY_SHRINKABLE;

  /**
   * Creates a ShrinkAnnotatedTypeFactory.
   *
   * @param checker the associated type-checker
   */
  @SuppressWarnings("this-escape")
  public ShrinkAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.MAYBE_SHRINKABLE = AnnotationBuilder.fromClass(elements, MaybeShrinkable.class);
    this.SHRINKABLE = AnnotationBuilder.fromClass(elements, Shrinkable.class);
    this.UNSHRINKABLE = AnnotationBuilder.fromClass(elements, Unshrinkable.class);
    this.POLY_SHRINKABLE = AnnotationBuilder.fromClass(elements, PolyShrinkable.class);
    postInit();
  }

  @Override
  protected AnnotationMirror topAnnotation() {
    return MAYBE_SHRINKABLE;
  }

  @Override
  protected AnnotationMirror positiveCapability() {
    return SHRINKABLE;
  }

  @Override
  protected AnnotationMirror negativeCapability() {
    return UNSHRINKABLE;
  }

  @Override
  protected AnnotationMirror polyCapability() {
    return POLY_SHRINKABLE;
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(
            MaybeShrinkable.class,
            Shrinkable.class,
            Unshrinkable.class,
            BottomShrinkable.class,
            PolyShrinkable.class));
  }

  /**
   * {@code Map.Entry} has no shrink methods, so {@code @Modifiable} and {@code @Unmodifiable} make
   * no claim about it.
   */
  @Override
  protected boolean typeLacksCapability(TypeMirror type) {
    if (type.getKind() != TypeKind.DECLARED) {
      return false;
    }
    return TypesUtils.isErasedSubtype(type, mapEntryErasure, types);
  }

  /**
   * For {@code Map.Entry}, only the replace bit is meaningful to carry from the map receiver, so
   * the shrink bit of {@code @PolyModifiable} is {@code @MaybeShrinkable}.
   */
  @Override
  protected boolean polyLacksCapability(TypeMirror type) {
    if (type.getKind() != TypeKind.DECLARED) {
      return false;
    }
    return TypesUtils.isErasedSubtype(type, mapEntryErasure, types);
  }

  @Override
  protected @Nullable TypeMirror refinedIteratorResultBound() {
    // Every Iterator can shrink, via remove().
    return iteratorErasure;
  }
}
