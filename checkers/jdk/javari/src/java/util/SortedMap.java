package java.util;
import checkers.javari.quals.*;

public interface SortedMap<K, V> extends java.util.Map<K, V> {
  public interface Entry<K, V> {
    public abstract K getKey() @ReadOnly;
    public abstract V getValue() @ReadOnly;
    public abstract V setValue(V a1);
    public abstract boolean equals(@ReadOnly java.lang.Object a1) @ReadOnly;
    public abstract int hashCode() @ReadOnly;
  }
  public abstract java.util.Comparator<? super K> comparator() @ReadOnly;
  public abstract @PolyRead java.util.SortedMap<K, V> subMap(K a1, K a2) @PolyRead;
  public abstract @PolyRead java.util.SortedMap<K, V> headMap(K a1) @PolyRead;
  public abstract @PolyRead java.util.SortedMap<K, V> tailMap(K a1) @PolyRead;
  public abstract K firstKey() @ReadOnly;
  public abstract K lastKey() @ReadOnly;
  public abstract @PolyRead java.util.Set<K> keySet() @PolyRead;
  public abstract @PolyRead java.util.Collection<V> values() @PolyRead;
  public abstract @PolyRead java.util.Set<@PolyRead java.util.Map.Entry<K, V>> entrySet() @PolyRead;
}
