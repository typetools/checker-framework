package java.util;


import org.checkerframework.checker.lock.qual.*;

// This class allows null elements
public class IdentityHashMap<K extends Object, V extends Object> extends AbstractMap<K, V> implements Map<K, V>, java.io.Serializable, Cloneable {
  private static final long serialVersionUID = 0;
  public IdentityHashMap() { throw new RuntimeException("skeleton method"); }
  public IdentityHashMap(int a1) { throw new RuntimeException("skeleton method"); }
  public IdentityHashMap(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
   public int size(@GuardSatisfied IdentityHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public boolean isEmpty(@GuardSatisfied IdentityHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public V get(@GuardSatisfied IdentityHashMap<K,V> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public boolean containsKey(@GuardSatisfied IdentityHashMap<K,V> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public boolean containsValue(@GuardSatisfied IdentityHashMap<K,V> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public V put(K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public void putAll(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public V remove(Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
   public boolean equals(@GuardSatisfied IdentityHashMap<K,V> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied IdentityHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Set<K> keySet(@GuardSatisfied IdentityHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Collection<V> values(@GuardSatisfied IdentityHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Set<Map.Entry<K,V>> entrySet(@GuardSatisfied IdentityHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Object clone(@GuardSatisfied IdentityHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
}
