package java.util;
import checkers.javari.quals.*;

public interface SortedMap<K, V> extends Map<K, V> {
  public abstract Comparator<? super K> comparator() @ReadOnly;
  public abstract @PolyRead SortedMap<K, V> subMap(K a1, K a2) @PolyRead;
  public abstract @PolyRead SortedMap<K, V> headMap(K a1) @PolyRead;
  public abstract @PolyRead SortedMap<K, V> tailMap(K a1) @PolyRead;
  public abstract K firstKey() @ReadOnly;
  public abstract K lastKey() @ReadOnly;
  public abstract @PolyRead Set<K> keySet() @PolyRead;
  public abstract @PolyRead Collection<V> values() @PolyRead;
  public abstract @PolyRead Set<@PolyRead Map.Entry<K, V>> entrySet() @PolyRead;
}
