package org.checkerframework.checker.modifiability.seqgrow;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.modifiability.ModifiabilityBaseAnnotatedTypeFactory;
import org.checkerframework.checker.modifiability.qual.BottomSeqGrowable;
import org.checkerframework.checker.modifiability.qual.MaybeSeqGrowable;
import org.checkerframework.checker.modifiability.qual.PolySeqGrowable;
import org.checkerframework.checker.modifiability.qual.SeqGrowable;
import org.checkerframework.checker.modifiability.qual.SeqUngrowable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.TypesUtils;

/** The annotated type factory for the {@link SeqGrowChecker}. */
public class SeqGrowAnnotatedTypeFactory extends ModifiabilityBaseAnnotatedTypeFactory {

  /** The erased {@code java.util.SequencedCollection} type, or null on a JDK before Java 21. */
  private final @Nullable TypeMirror sequencedCollectionErasure;

  /** The erased {@code java.util.SequencedMap} type, or null on a JDK before Java 21. */
  private final @Nullable TypeMirror sequencedMapErasure;

  /** The erased {@code java.util.Deque} type. */
  private final TypeMirror dequeErasure;

  /**
   * Creates a SeqGrowAnnotatedTypeFactory.
   *
   * @param checker the associated type-checker
   */
  @SuppressWarnings("this-escape")
  public SeqGrowAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(
        checker,
        MaybeSeqGrowable.class,
        SeqGrowable.class,
        SeqUngrowable.class,
        BottomSeqGrowable.class,
        PolySeqGrowable.class);
    this.sequencedCollectionErasure = optionalErasureOf("java.util.SequencedCollection");
    this.sequencedMapErasure = optionalErasureOf("java.util.SequencedMap");
    this.dequeErasure = erasureOf("java.util.Deque");
    postInit();
  }

  /**
   * Returns the erasure of the named type, or null if the type is not present on this JDK.
   *
   * @param canonicalName the canonical name of a type that may not be present
   * @return the erasure of the named type, or null
   */
  private @Nullable TypeMirror optionalErasureOf(
      @UnderInitialization(ModifiabilityBaseAnnotatedTypeFactory.class) SeqGrowAnnotatedTypeFactory this,
      @FullyQualifiedName String canonicalName) {
    TypeElement element = elements.getTypeElement(canonicalName);
    return element == null ? null : types.erasure(element.asType());
  }

  /**
   * Only sequenced collections and sequenced maps have sequenced-grow methods, so
   * {@code @Modifiable} and {@code @Unmodifiable} make no claim about any other type. This is an
   * allowlist, unlike the blocklists of the other capabilities.
   *
   * <p>On JDKs before Java 21, {@code SequencedCollection} and {@code SequencedMap} are not
   * present, but {@code Deque} still has first/last insertion methods.
   */
  @Override
  protected boolean typeLacksCapability(TypeMirror type) {
    if (type.getKind() != TypeKind.DECLARED) {
      return true;
    }
    boolean canSeqGrow =
        (sequencedCollectionErasure != null
                && TypesUtils.isErasedSubtype(type, sequencedCollectionErasure, types))
            || (sequencedMapErasure != null
                && TypesUtils.isErasedSubtype(type, sequencedMapErasure, types))
            || TypesUtils.isErasedSubtype(type, dequeErasure, types);
    return !canSeqGrow;
  }

  @Override
  protected List<TypeMirror> typesWithCapability() {
    List<TypeMirror> result = new ArrayList<>(3);
    if (sequencedCollectionErasure != null) {
      result.add(sequencedCollectionErasure);
    }
    if (sequencedMapErasure != null) {
      result.add(sequencedMapErasure);
    }
    result.add(dequeErasure);
    return result;
  }

  /**
   * For {@code Map.Entry}, only the replace bit is meaningful to carry from the map receiver, so
   * the sequenced-grow bit of {@code @PolyModifiable} is {@code @MaybeSeqGrowable}.
   */
  @Override
  protected boolean polyLacksCapability(TypeMirror type) {
    if (type.getKind() != TypeKind.DECLARED) {
      return false;
    }
    return TypesUtils.isErasedSubtype(type, mapEntryErasure, types);
  }
}
