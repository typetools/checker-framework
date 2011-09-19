package java.util;
import checkers.javari.quals.*;

public class IdentityHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, java.io.Serializable, Cloneable {
    private static final long serialVersionUID = 0L;
  public IdentityHashMap() { throw new RuntimeException(("skeleton method")); }
  public IdentityHashMap(int a1) { throw new RuntimeException(("skeleton method")); }
  public IdentityHashMap(@PolyRead IdentityHashMap this, @PolyRead Map<? extends K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public int size(@ReadOnly IdentityHashMap this) { throw new RuntimeException(("skeleton method")); }
  public boolean isEmpty(@ReadOnly IdentityHashMap this) { throw new RuntimeException(("skeleton method")); }
  public V get(@ReadOnly IdentityHashMap this, Object a1) { throw new RuntimeException(("skeleton method")); }
  public boolean containsKey(@ReadOnly IdentityHashMap this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public boolean containsValue(@ReadOnly IdentityHashMap this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public V put(K a1, V a2) { throw new RuntimeException(("skeleton method")); }
  public void putAll(@ReadOnly Map<? extends K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public V remove(@ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public boolean equals(@ReadOnly IdentityHashMap this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public int hashCode(@ReadOnly IdentityHashMap this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Set<K> keySet(@PolyRead IdentityHashMap this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Collection<V> values(@PolyRead IdentityHashMap this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Set<@PolyRead Map.Entry<K, V>> entrySet(@PolyRead IdentityHashMap this) { throw new RuntimeException(("skeleton method")); }
  public Object clone() { throw new RuntimeException("skeleton method"); }
}
