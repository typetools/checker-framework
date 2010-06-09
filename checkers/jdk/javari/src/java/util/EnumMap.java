package java.util;
import checkers.javari.quals.*;

public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> implements java.io.Serializable, Cloneable {
    private static final long serialVersionUID = 0L;
  public EnumMap(Class<K> a1) { throw new RuntimeException(("skeleton method")); }
  public EnumMap(@PolyRead EnumMap<K, ? extends V> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public EnumMap(@PolyRead Map<K, ? extends V> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public int size() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean containsValue(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean containsKey(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public V get(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public V put(K a1, V a2) { throw new RuntimeException(("skeleton method")); }
  public V remove(@ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public void putAll(@ReadOnly Map<? extends K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Set<K> keySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Collection<V> values() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Set<Map.Entry<K, V>> entrySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public boolean equals(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public EnumMap<K, V> clone() { throw new RuntimeException("skeleton method"); }
}
