package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// This collection can only contain nonnull values
public class Hashtable<K extends @NonNull Object, V extends @NonNull Object> extends java.util.Dictionary<K, V> implements java.util.Map<K, V>, java.lang.Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public Hashtable(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public Hashtable(int a1) { throw new RuntimeException("skeleton method"); }
  public Hashtable() { throw new RuntimeException("skeleton method"); }
  public Hashtable(java.util.Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int size() { throw new RuntimeException("skeleton method"); }
  public synchronized boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public synchronized java.util.Enumeration<K> keys() { throw new RuntimeException("skeleton method"); }
  public synchronized java.util.Enumeration<V> elements() { throw new RuntimeException("skeleton method"); }
  public synchronized boolean contains(java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean containsKey(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized @Nullable V get(java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized @Nullable V put(K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public synchronized @Nullable V remove(java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void putAll(java.util.Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void clear() { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public java.util.Set<K> keySet() { throw new RuntimeException("skeleton method"); }
  public java.util.Set<java.util.Map.Entry<K, V>> entrySet() { throw new RuntimeException("skeleton method"); }
  public java.util.Collection<V> values() { throw new RuntimeException("skeleton method"); }
  public synchronized boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int hashCode() { throw new RuntimeException("skeleton method"); }
  public synchronized Object clone() { throw new RuntimeException("skeleton method"); }

}
