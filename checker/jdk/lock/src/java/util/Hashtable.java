package java.util;



import org.checkerframework.checker.lock.qual.*;

// This collection can only contain nonnull values
public class Hashtable<K extends Object, V extends Object> extends Dictionary<K, V> implements Map<K, V>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public Hashtable(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public Hashtable(int a1) { throw new RuntimeException("skeleton method"); }
  public Hashtable() { throw new RuntimeException("skeleton method"); }
  public Hashtable(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
   public synchronized int size(@GuardSatisfied Hashtable<K,V> this) { throw new RuntimeException("skeleton method"); }
   public synchronized boolean isEmpty(@GuardSatisfied Hashtable<K,V> this) { throw new RuntimeException("skeleton method"); }
  public synchronized Enumeration<K> keys() { throw new RuntimeException("skeleton method"); }
  public synchronized Enumeration<V> elements() { throw new RuntimeException("skeleton method"); }
  public synchronized boolean contains(@GuardSatisfied Hashtable<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@GuardSatisfied Hashtable<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean containsKey(@GuardSatisfied Hashtable<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized V get(@GuardSatisfied Hashtable<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized V put(@GuardSatisfied Hashtable<K,V> this, K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public synchronized V remove(@GuardSatisfied Hashtable<K,V> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void putAll(@GuardSatisfied Hashtable<K,V> this, Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void clear(@GuardSatisfied Hashtable<K,V> this) { throw new RuntimeException("skeleton method"); }
   public synchronized String toString(@GuardSatisfied Hashtable<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Set<K> keySet(@GuardSatisfied Hashtable<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Set<Map.Entry<K,V>> entrySet(@GuardSatisfied Hashtable<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Collection<V> values(@GuardSatisfied Hashtable<K,V> this) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean equals(@GuardSatisfied Hashtable<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public synchronized int hashCode(@GuardSatisfied Hashtable<K,V> this) { throw new RuntimeException("skeleton method"); }
   public synchronized Object clone(@GuardSatisfied Hashtable<K,V> this) { throw new RuntimeException("skeleton method"); }

}
