package java.util;
import checkers.javari.quals.*;

public interface NavigableMap<K, V> extends SortedMap<K, V> {
  public abstract @PolyRead Map.Entry<K, V> lowerEntry(K a1) @PolyRead;
  public abstract K lowerKey(K a1) @ReadOnly;
  public abstract @PolyRead Map.Entry<K, V> floorEntry(K a1) @PolyRead;
  public abstract K floorKey(K a1) @ReadOnly;
  public abstract @PolyRead Map.Entry<K, V> ceilingEntry(K a1) @PolyRead;
  public abstract K ceilingKey(K a1) @ReadOnly;
  public abstract @PolyRead Map.Entry<K, V> higherEntry(K a1) @PolyRead;
  public abstract K higherKey(K a1) @ReadOnly;
  public abstract @PolyRead Map.Entry<K, V> firstEntry() @PolyRead;
  public abstract @PolyRead Map.Entry<K, V> lastEntry() @PolyRead;
  public abstract Map.Entry<K, V> pollFirstEntry();
  public abstract Map.Entry<K, V> pollLastEntry();
  public abstract @PolyRead NavigableMap<K, V> descendingMap() @PolyRead;
  public abstract @PolyRead NavigableSet<K> navigableKeySet() @PolyRead;
  public abstract @PolyRead NavigableSet<K> descendingKeySet() @PolyRead;
  public abstract @PolyRead NavigableMap<K, V> subMap(K a1, boolean a2, K a3, boolean a4) @PolyRead;
  public abstract @PolyRead NavigableMap<K, V> headMap(K a1, boolean a2) @PolyRead;
  public abstract @PolyRead NavigableMap<K, V> tailMap(K a1, boolean a2) @PolyRead;
  public abstract @PolyRead SortedMap<K, V> subMap(K a1, K a2) @PolyRead;
  public abstract @PolyRead SortedMap<K, V> headMap(K a1) @PolyRead;
  public abstract @PolyRead SortedMap<K, V> tailMap(K a1) @PolyRead;
}
