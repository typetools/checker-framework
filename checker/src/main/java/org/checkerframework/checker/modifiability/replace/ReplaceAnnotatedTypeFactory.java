package org.checkerframework.checker.modifiability.replace;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.modifiability.ModifiabilityBaseAnnotatedTypeFactory;
import org.checkerframework.checker.modifiability.qual.BottomReplaceable;
import org.checkerframework.checker.modifiability.qual.MaybeReplaceable;
import org.checkerframework.checker.modifiability.qual.PolyReplaceable;
import org.checkerframework.checker.modifiability.qual.Replaceable;
import org.checkerframework.checker.modifiability.qual.Unreplaceable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TypesUtils;

/** The annotated type factory for the {@link ReplaceChecker}. */
public class ReplaceAnnotatedTypeFactory extends ModifiabilityBaseAnnotatedTypeFactory {

  /** The erased {@code java.util.Set} type. */
  private final TypeMirror setErasure;

  /** The erased {@code java.util.Queue} type. */
  private final TypeMirror queueErasure;

  /** The erased {@code java.util.List} type. */
  private final TypeMirror listErasure;

  /** The {@code @}{@link MaybeReplaceable} qualifier. */
  private final AnnotationMirror MAYBE_REPLACEABLE;

  /** The {@code @}{@link Replaceable} qualifier. */
  private final AnnotationMirror REPLACEABLE;

  /** The {@code @}{@link Unreplaceable} qualifier. */
  private final AnnotationMirror UNREPLACEABLE;

  /** The {@code @}{@link PolyReplaceable} qualifier. */
  private final AnnotationMirror POLY_REPLACEABLE;

  /**
   * Creates a ReplaceAnnotatedTypeFactory.
   *
   * @param checker the associated type-checker
   */
  @SuppressWarnings("this-escape")
  public ReplaceAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.setErasure = erasureOf("java.util.Set");
    this.queueErasure = erasureOf("java.util.Queue");
    this.listErasure = erasureOf("java.util.List");
    this.MAYBE_REPLACEABLE = AnnotationBuilder.fromClass(elements, MaybeReplaceable.class);
    this.REPLACEABLE = AnnotationBuilder.fromClass(elements, Replaceable.class);
    this.UNREPLACEABLE = AnnotationBuilder.fromClass(elements, Unreplaceable.class);
    this.POLY_REPLACEABLE = AnnotationBuilder.fromClass(elements, PolyReplaceable.class);
    postInit();
  }

  @Override
  protected AnnotationMirror topAnnotation() {
    return MAYBE_REPLACEABLE;
  }

  @Override
  protected AnnotationMirror positiveCapability() {
    return REPLACEABLE;
  }

  @Override
  protected AnnotationMirror negativeCapability() {
    return UNREPLACEABLE;
  }

  @Override
  protected AnnotationMirror polyCapability() {
    return POLY_REPLACEABLE;
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(
            MaybeReplaceable.class,
            Replaceable.class,
            Unreplaceable.class,
            BottomReplaceable.class,
            PolyReplaceable.class));
  }

  /**
   * An exact {@code Collection}, a {@code Set}, a {@code Queue} that is not a {@code List}, and a
   * non-{@code ListIterator} {@code Iterator} have no replace methods, so {@code @Modifiable} and
   * {@code @Unmodifiable} make no claim about them.
   */
  @Override
  protected boolean typeLacksCapability(TypeMirror type) {
    if (type.getKind() != TypeKind.DECLARED) {
      return false;
    }
    return types.isSameType(types.erasure(type), collectionErasure)
        || TypesUtils.isErasedSubtype(type, setErasure, types)
        || (TypesUtils.isErasedSubtype(type, queueErasure, types)
            && !TypesUtils.isErasedSubtype(type, listErasure, types))
        || (TypesUtils.isErasedSubtype(type, iteratorErasure, types)
            && !TypesUtils.isErasedSubtype(type, listIteratorErasure, types));
  }

  @Override
  protected List<TypeMirror> typesWithCapability() {
    return List.of(listErasure, listIteratorErasure, mapErasure, mapEntryErasure);
  }

  // polyLacksCapability is not overridden: unlike grow and shrink for Map.Entry, replacement
  // through Entry.setValue is a meaningful capability to carry from the map receiver.

  @Override
  protected @Nullable TypeMirror refinedIteratorResultBound() {
    // A plain Iterator cannot replace, so only a ListIterator result is refined.
    return listIteratorErasure;
  }
}
