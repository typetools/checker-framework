package java.util;
import checkers.javari.quals.*;

public class WeakHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
  public WeakHashMap(int a1, float a2) { throw new RuntimeException(("skeleton method")); }
  public WeakHashMap(int a1) { throw new RuntimeException(("skeleton method")); }
  public WeakHashMap() { throw new RuntimeException(("skeleton method")); }
  public WeakHashMap(@PolyRead Map<? extends K, ? extends V> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public int size() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean isEmpty() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public V get(@ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public boolean containsKey(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public V put(K a1, V a2) { throw new RuntimeException(("skeleton method")); }
  public void putAll(@ReadOnly Map<? extends K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public V remove(@ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public boolean containsValue(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Set<K> keySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Collection<V> values() @PolyRead{ throw new RuntimeException(("skeleton method")); }
  public @PolyRead Set<Map.Entry<K, V>> entrySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
}
