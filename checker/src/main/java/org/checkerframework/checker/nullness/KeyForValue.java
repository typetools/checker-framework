package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
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
   * If the underlying type is a type variable or a wildcard, then this is a set of maps for which
   * this value is a key. Otherwise, it's null.
   */
  // Cannot be final because lub re-assigns; add a new constructor to do this cleanly?
  private @Nullable Set<String> keyForMaps;

  /** Create an instance. */
  public KeyForValue(
      CFAbstractAnalysis<KeyForValue, ?, ?> analysis,
      Set<AnnotationMirror> annotations,
      TypeMirror underlyingType) {
    super(analysis, annotations, underlyingType);
    KeyForAnnotatedTypeFactory atypeFactory =
        (KeyForAnnotatedTypeFactory) analysis.getTypeFactory();
    AnnotationMirror keyfor = atypeFactory.getAnnotationByClass(annotations, KeyFor.class);
    if (keyfor != null
        && (underlyingType.getKind() == TypeKind.TYPEVAR
            || underlyingType.getKind() == TypeKind.WILDCARD)) {
      List<String> list =
          AnnotationUtils.getElementValueArray(
              keyfor, atypeFactory.keyForValueElement, String.class);
      keyForMaps = new LinkedHashSet<>(list.size());
      keyForMaps.addAll(list);
    } else {
      keyForMaps = null;
    }
  }

  /**
   * If the underlying type is a type variable or a wildcard, then this is a set of maps for which
   * this value is a key. Otherwise, it's null.
   */
  public Set<String> getKeyForMaps() {
    return keyForMaps;
  }

  @Override
  public KeyForValue leastUpperBound(KeyForValue other) {
    KeyForValue lub = super.leastUpperBound(other);
    if (other == null || other.keyForMaps == null || this.keyForMaps == null) {
      return lub;
    }
    // Lub the keyForMaps by intersecting the sets.
    lub.keyForMaps = new LinkedHashSet<>(this.keyForMaps.size());
    lub.keyForMaps.addAll(this.keyForMaps);
    lub.keyForMaps.retainAll(other.keyForMaps);
    if (lub.keyForMaps.isEmpty()) {
      lub.keyForMaps = null;
    }
    return lub;
  }

  @Override
  public KeyForValue mostSpecific(KeyForValue other, KeyForValue backup) {
    KeyForValue mostSpecific = super.mostSpecific(other, backup);
    if (mostSpecific == null) {
      if (other == null) {
        return this;
      }
      // mostSpecific is null if the two types are not comparable.  This is normally
      // because one of this or other is a type variable and annotations is empty, but the
      // other annotations are not empty.  In this case, copy the keyForMaps and to the
      // value with the no annotations and return it as most specific.
      if (other.getAnnotations().isEmpty()) {
        other.addKeyFor(this.keyForMaps);
        return other;
      } else if (this.getAnnotations().isEmpty()) {
        this.addKeyFor(other.keyForMaps);
        return this;
      }
      return null;
    }

    mostSpecific.addKeyFor(this.keyForMaps);
    if (other != null) {
      mostSpecific.addKeyFor(other.keyForMaps);
    }
    return mostSpecific;
  }

  private void addKeyFor(Set<String> newKeyForMaps) {
    if (newKeyForMaps == null || newKeyForMaps.isEmpty()) {
      return;
    }
    if (keyForMaps == null) {
      keyForMaps = new LinkedHashSet<>();
    }
    keyForMaps.addAll(newKeyForMaps);
  }
}
