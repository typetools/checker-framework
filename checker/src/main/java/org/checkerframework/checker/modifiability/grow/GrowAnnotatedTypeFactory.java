package org.checkerframework.checker.modifiability.grow;

import java.util.List;
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
import org.checkerframework.javacutil.TypesUtils;

/** The annotated type factory for the {@link GrowChecker}. */
public class GrowAnnotatedTypeFactory extends ModifiabilityBaseAnnotatedTypeFactory {

  /**
   * Creates a GrowAnnotatedTypeFactory.
   *
   * @param checker the associated type-checker
   */
  @SuppressWarnings("this-escape")
  public GrowAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(
        checker,
        MaybeGrowable.class,
        Growable.class,
        Ungrowable.class,
        BottomGrowable.class,
        PolyGrowable.class);
    postInit();
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
