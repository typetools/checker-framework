package java.util;
import checkers.igj.quals.*;

@I
public class IdentityHashMap<K, V> extends @I java.util.AbstractMap<K, V> implements @I java.util.Map<K, V>, @I java.io.Serializable, @I java.lang.Cloneable {
  public IdentityHashMap() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public IdentityHashMap(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public IdentityHashMap(@ReadOnly java.util.Map<? extends K, ? extends V> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public V get(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean containsKey(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public V put(K a1, V a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public void putAll(@ReadOnly java.util.Map<? extends K, ? extends V> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public V remove(@ReadOnly java.lang.Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void clear() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.Set<K> keySet() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.Collection<V> values() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I java.util.Set<@I java.util.Map.Entry<K, V>> entrySet() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
