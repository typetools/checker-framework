package org.checkerframework.checker.modifiability.shrink;

import java.util.List;
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
import org.checkerframework.javacutil.TypesUtils;

/** The annotated type factory for the {@link ShrinkChecker}. */
public class ShrinkAnnotatedTypeFactory extends ModifiabilityBaseAnnotatedTypeFactory {

  /**
   * Creates a ShrinkAnnotatedTypeFactory.
   *
   * @param checker the associated type-checker
   */
  @SuppressWarnings("this-escape")
  public ShrinkAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(
        checker,
        MaybeShrinkable.class,
        Shrinkable.class,
        Unshrinkable.class,
        BottomShrinkable.class,
        PolyShrinkable.class);
    postInit();
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

  @Override
  protected List<TypeMirror> typesWithCapability() {
    return List.of(collectionErasure, mapErasure, iteratorErasure);
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
