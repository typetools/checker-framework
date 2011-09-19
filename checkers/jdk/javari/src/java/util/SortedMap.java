package java.util;
import checkers.javari.quals.*;

public interface SortedMap<K, V> extends Map<K, V> {
  public abstract Comparator<? super K> comparator(@ReadOnly SortedMap this);
  public abstract @PolyRead SortedMap<K, V> subMap(@PolyRead SortedMap this, K a1, K a2);
  public abstract @PolyRead SortedMap<K, V> headMap(@PolyRead SortedMap this, K a1);
  public abstract @PolyRead SortedMap<K, V> tailMap(@PolyRead SortedMap this, K a1);
  public abstract K firstKey(@ReadOnly SortedMap this);
  public abstract K lastKey(@ReadOnly SortedMap this);
  public abstract @PolyRead Set<K> keySet(@PolyRead SortedMap this);
  public abstract @PolyRead Collection<V> values(@PolyRead SortedMap this);
  public abstract @PolyRead Set<@PolyRead Map.Entry<K, V>> entrySet(@PolyRead SortedMap this);
}
