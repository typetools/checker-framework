package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * KeyForValue holds additional information about which maps this value is a key for. This extra
 * information is required when adding the @KeyFor qualifier to the type is not a refinement of the
 * type. For example,
 *
 * <pre>
 *     {@code @NonNull Object o = map.get(param);}
 * </pre>
 *
 * <pre><code>
 * Map&lt;T, Object&gt; map = ...;
 * &lt;T&gt; T method(T param) {
 *   if (map.contains(param)) {
 *    &nbsp;@NonNull Object o = map.get(param);
 *     return param;
 *   }
 * }
 * </code></pre>
 *
 * Inside the if statement, {@code param} is a key for "map". This would normally be represented as
 * {@code @KeyFor("map") T}, but this is not a subtype of {@code T}, so the type cannot be refined.
 * Instead, the value for {@code param} includes "map" in the list of keyForMaps. This information
 * is used in {@link KeyForAnnotatedTypeFactory#isKeyForMap(String, ExpressionTree)}.
 */
public class KeyForValue extends CFAbstractValue<KeyForValue> {
  /**
   * If the underlying type is a type variable or a wildcard, then this is the possibly-empty set of
   * maps for which this value is a key. Otherwise, it's null.
   */
  private final @Nullable Set<String> keyForMaps;

  /**
   * Create a KeyForValue.
   *
   * @param analysis the analysis
   * @param annotations the annotations
   * @param underlyingType the underlying type
   */
  public KeyForValue(
      CFAbstractAnalysis<KeyForValue, ?, ?> analysis,
      AnnotationMirrorSet annotations,
      TypeMirror underlyingType) {
    super(analysis, annotations, underlyingType);
    KeyForAnnotatedTypeFactory atypeFactory =
        (KeyForAnnotatedTypeFactory) analysis.getTypeFactory();
    if (underlyingType.getKind() == TypeKind.TYPEVAR
        || underlyingType.getKind() == TypeKind.WILDCARD) {
      AnnotationMirror keyfor = atypeFactory.getAnnotationByClass(annotations, KeyFor.class);
      if (keyfor == null) {
        keyForMaps = new LinkedHashSet<>();
      } else {
        keyForMaps =
            new LinkedHashSet<>(
                AnnotationUtils.getElementValueArray(
                    keyfor, atypeFactory.keyForValueElement, String.class));
      }
    } else {
      keyForMaps = null;
    }
  }

  /**
   * Create a KeyForValue with the given keyForMaps.
   *
   * @param analysis the analysis
   * @param annotations the annotations
   * @param underlyingType the underlying type
   * @param keyForMaps the maps for which this value is a key, or null
   */
  private KeyForValue(
      CFAbstractAnalysis<KeyForValue, ?, ?> analysis,
      AnnotationMirrorSet annotations,
      TypeMirror underlyingType,
      @Nullable Set<String> keyForMaps) {
    super(analysis, annotations, underlyingType);
    this.keyForMaps = keyForMaps;
  }

  /**
   * Returns the maps for which this value is a key.
   *
   * @return if the underlying type is a type variable or a wildcard, then the possibly-empty set of
   *     maps for which this value is a key; otherwise, null
   */
  public @Nullable Set<String> getKeyForMaps() {
    return keyForMaps;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return super.equals(obj)
        && obj instanceof KeyForValue other
        && Objects.equals(keyForMaps, other.keyForMaps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), keyForMaps);
  }

  @Override
  protected KeyForValue upperBound(
      @Nullable KeyForValue other, TypeMirror upperBoundTypeMirror, boolean shouldWiden) {
    KeyForValue upperBound = super.upperBound(other, upperBoundTypeMirror, shouldWiden);

    if (other == null || other.keyForMaps == null || this.keyForMaps == null) {
      return upperBound;
    }
    // Lub the keyForMaps by intersecting the sets.
    Set<String> lubKeyForMaps = new LinkedHashSet<>(this.keyForMaps);
    lubKeyForMaps.retainAll(other.keyForMaps);
    return new KeyForValue(
        analysis, upperBound.getAnnotations(), upperBound.getUnderlyingType(), lubKeyForMaps);
  }

  @Override
  public @Nullable KeyForValue mostSpecific(
      @Nullable KeyForValue other, @Nullable KeyForValue backup) {
    KeyForValue mostSpecific = super.mostSpecific(other, backup);
    if (mostSpecific == null) {
      if (other == null) {
        return this;
      }
      // mostSpecific is null if the two types are not comparable.  This is normally
      // because one of this or other is a type variable and annotations is empty, but the
      // other annotations are not empty.  In this case, add the keyForMaps to the value with
      // no annotations and return it as most specific.
      if (other.getAnnotations().isEmpty()) {
        return other.withKeyFor(this.keyForMaps, null);
      } else if (this.getAnnotations().isEmpty()) {
        return this.withKeyFor(other.keyForMaps, null);
      }
      return null;
    }

    return mostSpecific.withKeyFor(this.keyForMaps, other == null ? null : other.keyForMaps);
  }

  /**
   * Returns a value that is like this one, but whose keyForMaps also contains {@code
   * newKeyForMaps1} and {@code newKeyForMaps2}. Does not side-effect this value, which might be
   * shared by multiple stores.
   *
   * @param newKeyForMaps1 maps to add, or null
   * @param newKeyForMaps2 more maps to add, or null
   * @return a value like this one whose keyForMaps also contains {@code newKeyForMaps1} and {@code
   *     newKeyForMaps2}; it is {@code this} if this value's keyForMaps already contains both
   */
  private KeyForValue withKeyFor(
      @Nullable Set<String> newKeyForMaps1, @Nullable Set<String> newKeyForMaps2) {
    Set<String> resultKeyForMaps = unionIfLarger(keyForMaps, newKeyForMaps1, newKeyForMaps2);
    if (resultKeyForMaps == null) {
      return this;
    }
    return new KeyForValue(analysis, getAnnotations(), getUnderlyingType(), resultKeyForMaps);
  }
}
