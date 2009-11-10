package java.util;
import checkers.javari.quals.*;

public class EnumMap<K extends java.lang.Enum<K>, V> extends java.util.AbstractMap<K, V> implements java.io.Serializable, java.lang.Cloneable {
    private static final long serialVersionUID = 0L;
  public EnumMap(java.lang.Class<K> a1) { throw new RuntimeException(("skeleton method")); }
  public EnumMap(@PolyRead java.util.EnumMap<K, ? extends V> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public EnumMap(@PolyRead java.util.Map<K, ? extends V> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public int size() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean containsValue(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public boolean containsKey(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public V get(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public V put(K a1, V a2) { throw new RuntimeException(("skeleton method")); }
  public V remove(@ReadOnly java.lang.Object a1) { throw new RuntimeException(("skeleton method")); }
  public void putAll(@ReadOnly java.util.Map<? extends K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Set<K> keySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Collection<V> values() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Set<java.util.Map.Entry<K, V>> entrySet() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public boolean equals(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public EnumMap<K, V> clone() { throw new RuntimeException("skeleton method"); }
}
