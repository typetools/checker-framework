package org.checkerframework.javacutil;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.checkerframework.dataflow.qual.Pure;

/**
 * The Map interface defines some of its methods with respect to the equals method. This
 * implementation of Map violates those specifications, but fulfills the same property using {@link
 * AnnotationUtils#areSame}.
 *
 * <p>For example, the specification for the containsKey(Object key) method says: "returns true if
 * and only if this map contains a mapping for a key k such that (key == null ? k == null :
 * key.equals(k))." The specification for {@link AnnotationMirrorMap#containsKey} is "returns true
 * if and only if this map contains a mapping for a key k such that (key == null ? k == null :
 * AnnotationUtils.areSame(key, k))."
 *
 * <p>AnnotationMirror is an interface and not all implementing classes provide a correct equals
 * method; therefore, existing implementations of Map cannot be used.
 */
public class AnnotationMirrorMap<V> implements Map<@KeyFor("this") AnnotationMirror, V> {

  /** The actual map to which all work is delegated. */
  // Not final because makeUnmodifiable() can reassign it.
  private NavigableMap<@KeyFor("this") AnnotationMirror, V> shadowMap =
      new TreeMap<>(AnnotationUtils::compareAnnotationMirrors);

  /** The canonical unmodifiable empty set. */
  private static AnnotationMirrorMap<?> emptyMap = unmodifiableSet(Collections.emptyMap());

  /** Default constructor. */
  public AnnotationMirrorMap() {}

  /**
   * Creates an annotation mirror map and adds all the mappings in {@code copy}.
   *
   * @param copy a map whose contents should be copied to the newly created map
   */
  @SuppressWarnings({"this-escape", "nullness:method.invocation"}) // initialization in constructor
  public AnnotationMirrorMap(Map<AnnotationMirror, ? extends V> copy) {
    this();
    this.putAll(copy);
  }

  /**
   * Returns an unmodifiable AnnotationMirrorSet with the given elements.
   *
   * @param annos the annotation mirrors that will constitute the new unmodifable set
   * @return an unmodifiable AnnotationMirrorSet with the given elements
   * @param <V> the type of the values in the map
   */
  public static <V> AnnotationMirrorMap<V> unmodifiableSet(
      Map<AnnotationMirror, ? extends V> annos) {
    AnnotationMirrorMap<V> result = new AnnotationMirrorMap<>(annos);
    result.makeUnmodifiable();
    return result;
  }

  /**
   * Returns an empty set.
   *
   * @return an empty set
   * @param <V> the type of the values in the map
   */
  @SuppressWarnings("unchecked")
  public static <V> AnnotationMirrorMap<V> emptyMap() {
    return (AnnotationMirrorMap<V>) emptyMap;
  }

  /**
   * Make this set unmodifiable.
   *
   * @return this set
   */
  public @This AnnotationMirrorMap<V> makeUnmodifiable() {
    shadowMap = Collections.unmodifiableNavigableMap(shadowMap);
    return this;
  }

  @Override
  public int size() {
    return shadowMap.size();
  }

  @Override
  public boolean isEmpty() {
    return shadowMap.isEmpty();
  }

  @SuppressWarnings("keyfor:contracts.conditional.postcondition") // delegation
  @Override
  public boolean containsKey(Object key) {
    if (key instanceof AnnotationMirror) {
      return AnnotationUtils.containsSame(shadowMap.keySet(), (AnnotationMirror) key);
    } else {
      return false;
    }
  }

  @Override
  public boolean containsValue(Object value) {
    return shadowMap.containsValue(value);
  }

  @Override
  @Pure
  public @NotOwning @Nullable V get(Object key) {
    if (key instanceof AnnotationMirror) {
      AnnotationMirror keyAnno =
          AnnotationUtils.getSame(shadowMap.keySet(), (AnnotationMirror) key);
      if (keyAnno != null) {
        return shadowMap.get(keyAnno);
      }
    }
    return null;
  }

  @SuppressWarnings({
    "keyfor:contracts.postcondition",
    "keyfor:contracts.postcondition",
    "keyfor:argument"
  }) // delegation
  @Override
  public @NotOwning @Nullable V put(AnnotationMirror key, V value) {
    V pre = get(key);
    remove(key);
    shadowMap.put(key, value);
    return pre;
  }

  @Override
  public @Nullable V remove(Object key) {
    if (key instanceof AnnotationMirror) {
      AnnotationMirror keyAnno =
          AnnotationUtils.getSame(shadowMap.keySet(), (AnnotationMirror) key);
      if (keyAnno != null) {
        return shadowMap.remove(keyAnno);
      }
    }
    return null;
  }

  @Override
  public void putAll(Map<? extends AnnotationMirror, ? extends V> m) {
    for (Map.Entry<? extends AnnotationMirror, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    shadowMap.clear();
  }

  @Override
  public AnnotationMirrorSet keySet() {
    return new AnnotationMirrorSet(shadowMap.keySet());
  }

  @Override
  public Collection<V> values() {
    return shadowMap.values();
  }

  @SuppressWarnings("keyfor:return") // delegation
  @Override
  public Set<Map.Entry<@KeyFor("this") AnnotationMirror, V>> entrySet() {
    return shadowMap.entrySet();
  }

  @Override
  public String toString() {
    return shadowMap.toString();
  }

  @Override
  @Pure
  public boolean equals(@Nullable Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof AnnotationMirrorMap)) {
      return false;
    }
    AnnotationMirrorMap<?> m = (AnnotationMirrorMap) o;
    if (m.size() != size()) {
      return false;
    }

    try {
      for (Entry<AnnotationMirror, V> e : entrySet()) {
        AnnotationMirror key = e.getKey();
        V value = e.getValue();
        if (value == null) {
          if (!(m.get(key) == null && m.containsKey(key))) {
            return false;
          }
        } else {
          if (!value.equals(m.get(key))) {
            return false;
          }
        }
      }
    } catch (ClassCastException | NullPointerException unused) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = 0;
    for (Entry<AnnotationMirror, V> entry : entrySet()) {
      result += entry.hashCode();
    }
    return result;
  }
}
