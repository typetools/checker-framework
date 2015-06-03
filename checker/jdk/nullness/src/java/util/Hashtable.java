package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// This collection can only contain nonnull values
public class Hashtable<K extends @NonNull Object, V extends @NonNull Object> extends Dictionary<K, V> implements Map<K, V>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public Hashtable(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public Hashtable(int a1) { throw new RuntimeException("skeleton method"); }
  public Hashtable() { throw new RuntimeException("skeleton method"); }
  public Hashtable(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  @Pure public synchronized int size() { throw new RuntimeException("skeleton method"); }
  @Pure public synchronized boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public synchronized Enumeration<K> keys() { throw new RuntimeException("skeleton method"); }
  public synchronized Enumeration<V> elements() { throw new RuntimeException("skeleton method"); }
  @Pure public synchronized boolean contains(Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean containsValue(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public synchronized boolean containsKey(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public synchronized @Nullable V get(Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized @Nullable V put(K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public synchronized @Nullable V remove(Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void putAll(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void clear() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public synchronized String toString() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Set<@KeyFor("this") K> keySet() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Set<Map.Entry<@KeyFor("this") K, V>> entrySet() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Collection<V> values() { throw new RuntimeException("skeleton method"); }
  @Pure public synchronized boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public synchronized int hashCode() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public synchronized Object clone() { throw new RuntimeException("skeleton method"); }

}
