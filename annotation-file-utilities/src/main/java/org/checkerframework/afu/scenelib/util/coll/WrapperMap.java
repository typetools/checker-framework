package org.checkerframework.afu.scenelib.util.coll;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A {@link WrapperMap} is a map all of whose methods delegate by default to those of a supplied
 * {@linkplain #back backing map}. Subclasses can add or override methods. Compare to {@link
 * java.io.FilterInputStream}.
 */
public class WrapperMap<K, V> implements Map<K, V> {
  /** The backing map. */
  protected final Map<K, V> back;

  /** Constructs a new {@link WrapperMap} with the given backing map. */
  protected WrapperMap(Map<K, V> back) {
    this.back = back;
  }

  @Override
  public void clear() {
    back.clear();
  }

  @Override
  public boolean containsKey(Object key) {
    return back.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return back.containsValue(value);
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    return back.entrySet();
  }

  @Override
  public V get(Object key) {
    return back.get(key);
  }

  @Override
  public boolean isEmpty() {
    return back.isEmpty();
  }

  @Override
  public Set<K> keySet() {
    return back.keySet();
  }

  @Override
  public V put(K key, V value) {
    return back.put(key, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    back.putAll(m);
  }

  @Override
  public V remove(Object key) {
    return back.remove(key);
  }

  @Override
  public int size() {
    return back.size();
  }

  @Override
  public Collection<V> values() {
    return back.values();
  }

  @Override
  public boolean equals(Object o) {
    return back.equals(o);
  }

  @Override
  public int hashCode() {
    return back.hashCode();
  }

  @Override
  public String toString() {
    return back.toString();
  }
}
