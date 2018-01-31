package java.util;



import org.checkerframework.checker.lock.qual.*;

// permits null keys and values
public class WeakHashMap<K extends Object, V extends Object> extends AbstractMap<K, V> implements Map<K, V> {
  public WeakHashMap(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public WeakHashMap(int a1) { throw new RuntimeException("skeleton method"); }
  public WeakHashMap() { throw new RuntimeException("skeleton method"); }
  public WeakHashMap(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
   public int size(@GuardSatisfied WeakHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public boolean isEmpty(@GuardSatisfied WeakHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
  public V get(@GuardSatisfied WeakHashMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean containsKey(@GuardSatisfied WeakHashMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public V put(@GuardSatisfied WeakHashMap<K,V> this, K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public void putAll(@GuardSatisfied WeakHashMap<K,V> this, Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public V remove(@GuardSatisfied WeakHashMap<K,V> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@GuardSatisfied WeakHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@GuardSatisfied WeakHashMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public Set<K> keySet(@GuardSatisfied WeakHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Collection<V> values(@GuardSatisfied WeakHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
   public Set<Map.Entry<K,V>> entrySet(@GuardSatisfied WeakHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
}
