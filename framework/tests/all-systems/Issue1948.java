// Test case for Issue 1948:
// https://github.com/typetools/checker-framework/issues/1948

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("all") // ensure no crash
public class Issue1948<
        K, V, E extends Issue1948.MyEntry<K, V, E>, S extends Issue1948.MyClass<K, V, E, S>>
    implements ConcurrentMap<K, V> {

  private Issue1948(MapMaker builder, InternalEntryHelper<K, V, E, S> entryHelper) {}

  /** Returns a fresh {@link Issue1948} as specified by the given {@code builder}. */
  static <K, V> Issue1948<K, V, ? extends MyEntry<K, V, ?>, ?> create(MapMaker builder) {
    return new Issue1948<>(builder, Helper.<K, V>instance());
  }

  interface MyEntry<K, V, E extends MyEntry<K, V, E>> {}

  abstract static class MyClass<K, V, E extends MyEntry<K, V, E>, S extends MyClass<K, V, E, S>> {}

  static final class MapMaker {}

  static final class Helper<K, V>
      implements InternalEntryHelper<
          K, V, StrongKeyStrongValueEntry<K, V>, StrongKeyStrongValueMyClass<K, V>> {
    static <K, V> Helper<K, V> instance() {
      return null;
    }
  }

  interface InternalEntryHelper<K, V, E extends MyEntry<K, V, E>, S> {}

  abstract static class StrongKeyStrongValueEntry<K, V>
      extends AbstractStrongKeyEntry<K, V, StrongKeyStrongValueEntry<K, V>>
      implements StrongValueEntry<K, V, StrongKeyStrongValueEntry<K, V>> {}

  abstract static class AbstractStrongKeyEntry<K, V, E extends MyEntry<K, V, E>>
      implements MyEntry<K, V, E> {}

  interface StrongValueEntry<K, V, E extends MyEntry<K, V, E>> extends MyEntry<K, V, E> {}

  abstract static class StrongKeyStrongValueMyClass<K, V>
      extends MyClass<K, V, StrongKeyStrongValueEntry<K, V>, StrongKeyStrongValueMyClass<K, V>> {}

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean containsKey(Object key) {
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    return false;
  }

  @Override
  public V get(Object key) {
    return null;
  }

  @Override
  public V put(K key, V value) {
    return null;
  }

  @Override
  public V remove(Object key) {
    return null;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {}

  @Override
  public void clear() {}

  @Override
  public Set<K> keySet() {
    return null;
  }

  @Override
  public Collection<V> values() {
    return null;
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return null;
  }

  @Override
  public V getOrDefault(Object key, V defaultValue) {
    return null;
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {}

  @Override
  public V putIfAbsent(K key, V value) {
    return null;
  }

  @Override
  public boolean remove(Object key, Object value) {
    return false;
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return false;
  }

  @Override
  public V replace(K key, V value) {
    return null;
  }

  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {}

  @Override
  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    return null;
  }

  @Override
  public V computeIfPresent(
      K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    return null;
  }

  @Override
  public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    return null;
  }

  @Override
  public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    return null;
  }
}
