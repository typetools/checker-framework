package org.checkerframework.checker.modifiability.grow;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.modifiability.ModifiabilityBaseAnnotatedTypeFactory;
import org.checkerframework.checker.modifiability.qual.BottomGrowable;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.MaybeGrowable;
import org.checkerframework.checker.modifiability.qual.PolyGrowable;
import org.checkerframework.checker.modifiability.qual.Ungrowable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TypesUtils;

/** The annotated type factory for the {@link GrowChecker}. */
public class GrowAnnotatedTypeFactory extends ModifiabilityBaseAnnotatedTypeFactory {

  /** The {@code @}{@link MaybeGrowable} qualifier. */
  private final AnnotationMirror MAYBE_GROWABLE;

  /** The {@code @}{@link Growable} qualifier. */
  private final AnnotationMirror GROWABLE;

  /** The {@code @}{@link Ungrowable} qualifier. */
  private final AnnotationMirror UNGROWABLE;

  /** The {@code @}{@link PolyGrowable} qualifier. */
  private final AnnotationMirror POLY_GROWABLE;

  /**
   * Creates a GrowAnnotatedTypeFactory.
   *
   * @param checker the associated type-checker
   */
  @SuppressWarnings("this-escape")
  public GrowAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.MAYBE_GROWABLE = AnnotationBuilder.fromClass(elements, MaybeGrowable.class);
    this.GROWABLE = AnnotationBuilder.fromClass(elements, Growable.class);
    this.UNGROWABLE = AnnotationBuilder.fromClass(elements, Ungrowable.class);
    this.POLY_GROWABLE = AnnotationBuilder.fromClass(elements, PolyGrowable.class);
    postInit();
  }

  @Override
  protected AnnotationMirror topAnnotation() {
    return MAYBE_GROWABLE;
  }

  @Override
  protected AnnotationMirror positiveCapability() {
    return GROWABLE;
  }

  @Override
  protected AnnotationMirror negativeCapability() {
    return UNGROWABLE;
  }

  @Override
  protected AnnotationMirror polyCapability() {
    return POLY_GROWABLE;
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(
            MaybeGrowable.class,
            Growable.class,
            Ungrowable.class,
            BottomGrowable.class,
            PolyGrowable.class));
  }

  /**
   * {@code Map.Entry} and a non-{@code ListIterator} {@code Iterator} have no grow methods, so
   * {@code @Modifiable} and {@code @Unmodifiable} make no claim about them.
   */
  @Override
  protected boolean typeLacksCapability(TypeMirror type) {
    if (type.getKind() != TypeKind.DECLARED) {
      return false;
    }
    return TypesUtils.isErasedSubtype(type, mapEntryErasure, types)
        || (TypesUtils.isErasedSubtype(type, iteratorErasure, types)
            && !TypesUtils.isErasedSubtype(type, listIteratorErasure, types));
  }

  @Override
  protected List<TypeMirror> typesWithCapability() {
    return List.of(collectionErasure, mapErasure, listIteratorErasure);
  }

  /**
   * For {@code Map.Entry}, only the replace bit is meaningful to carry from the map receiver, so
   * the grow bit of {@code @PolyModifiable} is {@code @MaybeGrowable}.
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
    // A plain Iterator cannot grow, so only a ListIterator result is refined.
    return listIteratorErasure;
  }
}
