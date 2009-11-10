package java.util;
import checkers.igj.quals.*;

@I
public class Hashtable<K, V> extends @I java.util.Dictionary<K, V> implements @I java.util.Map<K, V>, @I java.lang.Cloneable, @I java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public Hashtable(int a1, float a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Hashtable(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Hashtable() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Hashtable(@ReadOnly java.util.Map<? extends K, ? extends V> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public synchronized int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized java.util.Enumeration<K> keys() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized java.util.Enumeration<V> elements() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized boolean containsKey(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized V get(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized V put(K a1, V a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized V remove(java.lang.Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized void putAll(@ReadOnly java.util.Map<? extends K, ? extends V> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized void clear() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.Set<K> keySet() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.Set<@I java.util.Map.Entry<K, V>> entrySet() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.Collection<V> values() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized boolean equals(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
