package java.util;
import checkers.igj.quals.*;

@I
public class WeakHashMap<K, V> extends @I AbstractMap<K, V> implements @I Map<K, V> {
  public WeakHashMap(int a1, float a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public WeakHashMap(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public WeakHashMap() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public WeakHashMap(Map<? extends K, ? extends V> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public V get(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean containsKey(Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public V put(K a1, V a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public void putAll(@ReadOnly Map<? extends K, ? extends V> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public V remove(@ReadOnly Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Set<K> keySet() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Collection<V> values() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Set<@I Map.Entry<K, V>> entrySet() @ReadOnly { throw new RuntimeException("skeleton method"); }
}
