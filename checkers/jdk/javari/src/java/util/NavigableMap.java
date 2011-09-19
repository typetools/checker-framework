package java.util;
import checkers.javari.quals.*;

public interface NavigableMap<K, V> extends SortedMap<K, V> {
  public abstract @PolyRead Map.Entry<K, V> lowerEntry(@PolyRead NavigableMap this, K a1);
  public abstract K lowerKey(@ReadOnly NavigableMap this, K a1);
  public abstract @PolyRead Map.Entry<K, V> floorEntry(@PolyRead NavigableMap this, K a1);
  public abstract K floorKey(@ReadOnly NavigableMap this, K a1);
  public abstract @PolyRead Map.Entry<K, V> ceilingEntry(@PolyRead NavigableMap this, K a1);
  public abstract K ceilingKey(@ReadOnly NavigableMap this, K a1);
  public abstract @PolyRead Map.Entry<K, V> higherEntry(@PolyRead NavigableMap this, K a1);
  public abstract K higherKey(@ReadOnly NavigableMap this, K a1);
  public abstract @PolyRead Map.Entry<K, V> firstEntry(@PolyRead NavigableMap this);
  public abstract @PolyRead Map.Entry<K, V> lastEntry(@PolyRead NavigableMap this);
  public abstract Map.Entry<K, V> pollFirstEntry();
  public abstract Map.Entry<K, V> pollLastEntry();
  public abstract @PolyRead NavigableMap<K, V> descendingMap(@PolyRead NavigableMap this);
  public abstract @PolyRead NavigableSet<K> navigableKeySet(@PolyRead NavigableMap this);
  public abstract @PolyRead NavigableSet<K> descendingKeySet(@PolyRead NavigableMap this);
  public abstract @PolyRead NavigableMap<K, V> subMap(@PolyRead NavigableMap this, K a1, boolean a2, K a3, boolean a4);
  public abstract @PolyRead NavigableMap<K, V> headMap(@PolyRead NavigableMap this, K a1, boolean a2);
  public abstract @PolyRead NavigableMap<K, V> tailMap(@PolyRead NavigableMap this, K a1, boolean a2);
  public abstract @PolyRead SortedMap<K, V> subMap(@PolyRead NavigableMap this, K a1, K a2);
  public abstract @PolyRead SortedMap<K, V> headMap(@PolyRead NavigableMap this, K a1);
  public abstract @PolyRead SortedMap<K, V> tailMap(@PolyRead NavigableMap this, K a1);
}
