package java.util;
import checkers.javari.quals.*;

public class Hashtable<K, V> extends Dictionary<K, V> implements Map<K, V>, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public Hashtable(int a1, float a2) { throw new RuntimeException(("skeleton method")); }
  public Hashtable(int a1) { throw new RuntimeException(("skeleton method")); }
  public Hashtable() { throw new RuntimeException(("skeleton method")); }
  public Hashtable(@PolyRead Hashtable<K, V> this, @PolyRead Map<? extends K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized int size(@ReadOnly Hashtable<K, V> this) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean isEmpty(@ReadOnly Hashtable<K, V> this) { throw new RuntimeException(("skeleton method")); }
  public synchronized @PolyRead Enumeration<K> keys(@PolyRead Hashtable<K, V> this) { throw new RuntimeException(("skeleton method")); }
  public synchronized @PolyRead Enumeration<V> elements(@PolyRead Hashtable<K, V> this){ throw new RuntimeException(("skeleton method")); }
  public synchronized boolean contains(@ReadOnly Hashtable<K, V> this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public boolean containsValue(@ReadOnly Hashtable<K, V> this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean containsKey(@ReadOnly Hashtable<K, V> this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized V get(@ReadOnly Hashtable<K, V> this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized V put(K a1, V a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized V remove(@ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized void putAll(@ReadOnly Map<? extends K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized void clear() { throw new RuntimeException(("skeleton method")); }
  public synchronized String toString(@ReadOnly Hashtable<K, V> this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Set<K> keySet(@PolyRead Hashtable<K, V> this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Set<Map.Entry<K, V>> entrySet(@PolyRead Hashtable<K, V> this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Collection<V> values(@PolyRead Hashtable<K, V> this) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean equals(@ReadOnly Hashtable<K, V> this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized int hashCode(@ReadOnly Hashtable<K, V> this) { throw new RuntimeException(("skeleton method")); }
  public synchronized Object clone() { throw new RuntimeException("skeleton method"); }
}
