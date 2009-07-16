package java.util;
import checkers.javari.quals.*;

public class IdentityHashMap<K, V> extends java.util.AbstractMap<K, V> implements java.util.Map<K, V>, java.io.Serializable, java.lang.Cloneable {
  public IdentityHashMap() { throw new RuntimeException(("skeleton method")); }
  public IdentityHashMap(int a1) { throw new RuntimeException(("skeleton method")); }
  public IdentityHashMap(@PolyRead java.util.Map<? extends K, ? extends V> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public int size() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean isEmpty() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public V get(java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean containsKey(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean containsValue(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public V put(K a1, V a2) { throw new RuntimeException(("skeleton method")); }
  public void putAll(@ReadOnly java.util.Map<? extends K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public V remove(@ReadOnly java.lang.Object a1) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public boolean equals(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public int hashCode() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Set<K> keySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Collection<V> values() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Set<@PolyRead java.util.Map.Entry<K, V>> entrySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
}
