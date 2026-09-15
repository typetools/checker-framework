package org.checkerframework.checker.modifiability.seqgrow;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
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
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TypesUtils;

/** The annotated type factory for the {@link SeqGrowChecker}. */
public class SeqGrowAnnotatedTypeFactory extends ModifiabilityBaseAnnotatedTypeFactory {

  /** The erased {@code java.util.SequencedCollection} type, or null on a JDK before Java 21. */
  private final @Nullable TypeMirror sequencedCollectionErasure;

  /** The erased {@code java.util.SequencedMap} type, or null on a JDK before Java 21. */
  private final @Nullable TypeMirror sequencedMapErasure;

  /** The erased {@code java.util.Deque} type. */
  private final TypeMirror dequeErasure;

  /** The {@code @}{@link MaybeSeqGrowable} qualifier. */
  private final AnnotationMirror MAYBE_SEQ_GROWABLE;

  /** The {@code @}{@link SeqGrowable} qualifier. */
  private final AnnotationMirror SEQ_GROWABLE;

  /** The {@code @}{@link SeqUngrowable} qualifier. */
  private final AnnotationMirror SEQ_UNGROWABLE;

  /** The {@code @}{@link PolySeqGrowable} qualifier. */
  private final AnnotationMirror POLY_SEQ_GROWABLE;

  /**
   * Creates a SeqGrowAnnotatedTypeFactory.
   *
   * @param checker the associated type-checker
   */
  @SuppressWarnings("this-escape")
  public SeqGrowAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.sequencedCollectionErasure = optionalErasureOf("java.util.SequencedCollection");
    this.sequencedMapErasure = optionalErasureOf("java.util.SequencedMap");
    this.dequeErasure = erasureOf("java.util.Deque");
    this.MAYBE_SEQ_GROWABLE = AnnotationBuilder.fromClass(elements, MaybeSeqGrowable.class);
    this.SEQ_GROWABLE = AnnotationBuilder.fromClass(elements, SeqGrowable.class);
    this.SEQ_UNGROWABLE = AnnotationBuilder.fromClass(elements, SeqUngrowable.class);
    this.POLY_SEQ_GROWABLE = AnnotationBuilder.fromClass(elements, PolySeqGrowable.class);
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

  @Override
  protected AnnotationMirror topAnnotation() {
    return MAYBE_SEQ_GROWABLE;
  }

  @Override
  protected AnnotationMirror positiveCapability() {
    return SEQ_GROWABLE;
  }

  @Override
  protected AnnotationMirror negativeCapability() {
    return SEQ_UNGROWABLE;
  }

  @Override
  protected AnnotationMirror polyCapability() {
    return POLY_SEQ_GROWABLE;
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(
            MaybeSeqGrowable.class,
            SeqGrowable.class,
            SeqUngrowable.class,
            BottomSeqGrowable.class,
            PolySeqGrowable.class));
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
}
