package java.util;
import checkers.igj.quals.*;

@I
public class Hashtable<K, V> extends @I Dictionary<K, V> implements @I Map<K, V>, @I Cloneable, @I java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public Hashtable(int a1, float a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Hashtable(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Hashtable() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Hashtable(@ReadOnly Map<? extends K, ? extends V> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public synchronized int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized Enumeration<K> keys() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized Enumeration<V> elements() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized boolean contains(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized boolean containsKey(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized V get(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized V put(K a1, V a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized V remove(Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized void putAll(@ReadOnly Map<? extends K, ? extends V> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized void clear() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public synchronized String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Set<K> keySet() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Set<@I Map.Entry<K, V>> entrySet() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Collection<V> values() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized boolean equals(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
