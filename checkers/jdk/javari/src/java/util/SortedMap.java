package java.util;
import checkers.javari.quals.*;

public interface SortedMap<K, V> extends Map<K, V> {
  public abstract Comparator<? super K> comparator(@ReadOnly SortedMap<K, V> this);
  public abstract @PolyRead SortedMap<K, V> subMap(@PolyRead SortedMap<K, V> this, K a1, K a2);
  public abstract @PolyRead SortedMap<K, V> headMap(@PolyRead SortedMap<K, V> this, K a1);
  public abstract @PolyRead SortedMap<K, V> tailMap(@PolyRead SortedMap<K, V> this, K a1);
  public abstract K firstKey(@ReadOnly SortedMap<K, V> this);
  public abstract K lastKey(@ReadOnly SortedMap<K, V> this);
  public abstract @PolyRead Set<K> keySet(@PolyRead SortedMap<K, V> this);
  public abstract @PolyRead Collection<V> values(@PolyRead SortedMap<K, V> this);
  public abstract @PolyRead Set<@PolyRead Map.Entry<K, V>> entrySet(@PolyRead SortedMap<K, V> this);
}
