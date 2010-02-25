package java.util;
import checkers.javari.quals.*;

public class Hashtable<K, V> extends java.util.Dictionary<K, V> implements java.util.Map<K, V>, java.lang.Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public Hashtable(int a1, float a2) { throw new RuntimeException(("skeleton method")); }
  public Hashtable(int a1) { throw new RuntimeException(("skeleton method")); }
  public Hashtable() { throw new RuntimeException(("skeleton method")); }
  public Hashtable(@PolyRead java.util.Map<? extends K, ? extends V> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public synchronized int size() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean isEmpty() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized @PolyRead java.util.Enumeration<K> keys() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public synchronized @PolyRead java.util.Enumeration<V> elements() @PolyRead{ throw new RuntimeException(("skeleton method")); }
  public synchronized boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean containsValue(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean containsKey(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized V get(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized V put(K a1, V a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized V remove(@ReadOnly java.lang.Object a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized void putAll(@ReadOnly java.util.Map<? extends K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized void clear() { throw new RuntimeException(("skeleton method")); }
  public synchronized java.lang.String toString() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Set<K> keySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Set<java.util.Map.Entry<K, V>> entrySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Collection<V> values() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean equals(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized int hashCode() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized Object clone() { throw new RuntimeException("skeleton method"); }
}
