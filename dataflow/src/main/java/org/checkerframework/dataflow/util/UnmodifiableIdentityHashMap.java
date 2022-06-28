package org.checkerframework.dataflow.util;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

/**
 * A wrapper around an {@link IdentityHashMap} that makes it unmodifiable. All mutating operations
 * throw {@link UnsupportedOperationException}, and all other operations delegate to the underlying
 * map.
 *
 * <p>This class extends {@link IdentityHashMap} only so it is assignable to variables / fields of
 * static type {@link IdentityHashMap}. All valid operations are delegated to the wrapped map, and
 * any inherited state from the superclass is unused.
 */
@SuppressWarnings("keyfor")
public class UnmodifiableIdentityHashMap<K, V> extends IdentityHashMap<K, V> {

  private static final long serialVersionUID = -5147442142854693854L;

  /** The wrapped map. */
  private final IdentityHashMap<K, V> map;

  /**
   * Create an UnmodifiableIdentityHashMap. Clients should use {@link #wrap} instead.
   *
   * @param map the map to wrap
   */
  private UnmodifiableIdentityHashMap(IdentityHashMap<K, V> map) {
    this.map = map;
  }

  /**
   * Create an {@link UnmodifiableIdentityHashMap} wrapper for a map.
   *
   * @param map the map to wrap
   * @return the wrapper
   * @param <K> the key type
   * @param <V> the value type
   */
  public static <K, V> UnmodifiableIdentityHashMap<K, V> wrap(IdentityHashMap<K, V> map) {
    // avoid repeated wrapping
    if (map instanceof UnmodifiableIdentityHashMap) {
      return (UnmodifiableIdentityHashMap<K, V>) map;
    }
    return new UnmodifiableIdentityHashMap<>(map);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public @Nullable V get(@Nullable Object key) {
    return map.get(key);
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    return map.containsValue(value);
  }

  @Override
  public @Nullable V put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nullable V remove(@Nullable Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  @SuppressWarnings("IdentityHashMapUsage")
  public boolean equals(@Nullable Object o) {
    return map.equals(o);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public Object clone() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<K> keySet() {
    return Collections.unmodifiableSet(map.keySet());
  }

  @Override
  public Collection<V> values() {
    return Collections.unmodifiableCollection(map.values());
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return Collections.unmodifiableSet(map.entrySet());
  }

  // `action` has no side effects on the map, because it is only passed keys and values.
  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    map.forEach(action);
  }

  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return map.toString();
  }

  @Override
  public V getOrDefault(Object key, V defaultValue) {
    return map.getOrDefault(key, defaultValue);
  }

  @Override
  public V putIfAbsent(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object key, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V replace(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @PolyNull V computeIfAbsent(
      K key, Function<? super K, ? extends @PolyNull V> mappingFunction) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @PolyNull V computeIfPresent(
      K key, BiFunction<? super K, ? super V, ? extends @PolyNull V> remappingFunction) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @PolyNull V compute(
      K key, BiFunction<? super K, ? super V, ? extends @PolyNull V> remappingFunction) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @PolyNull V merge(
      K key, V value, BiFunction<? super V, ? super V, ? extends @PolyNull V> remappingFunction) {
    throw new UnsupportedOperationException();
  }
}
