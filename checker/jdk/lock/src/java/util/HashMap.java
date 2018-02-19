package java.util;


import org.checkerframework.checker.lock.qual.*;

public class HashMap<K extends Object, V extends Object> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public HashMap(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public HashMap(int a1) { throw new RuntimeException("skeleton method"); }
  public HashMap() { throw new RuntimeException("skeleton method"); }
  public HashMap(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
   public int size(@GuardSatisfied HashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public boolean isEmpty(@GuardSatisfied HashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
  public V get(@GuardSatisfied HashMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsKey(@GuardSatisfied HashMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public V put(@GuardSatisfied HashMap<K,V> this, K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public void putAll(@GuardSatisfied HashMap<K,V> this, Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public V remove(@GuardSatisfied HashMap<K,V> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@GuardSatisfied HashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@GuardSatisfied HashMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public Set<K> keySet(@GuardSatisfied HashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Collection<V> values(@GuardSatisfied HashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Set<Map.Entry<K,V>> entrySet(@GuardSatisfied HashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Object clone(@GuardSatisfied HashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
}
