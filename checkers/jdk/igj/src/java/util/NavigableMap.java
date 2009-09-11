package java.util;
import checkers.igj.quals.*;

@I
public interface NavigableMap<K, V> extends @I java.util.SortedMap<K, V> {
  public abstract @I java.util.Map.Entry<K, V> lowerEntry(K a1) @ReadOnly;
  public abstract K lowerKey(K a1) @ReadOnly;
  public abstract @I java.util.Map.Entry<K, V> floorEntry(K a1) @ReadOnly;
  public abstract K floorKey(K a1) @ReadOnly;
  public abstract @I java.util.Map.Entry<K, V> ceilingEntry(K a1) @ReadOnly;
  public abstract K ceilingKey(K a1) @ReadOnly;
  public abstract @I java.util.Map.Entry<K, V> higherEntry(K a1) @ReadOnly;
  public abstract K higherKey(K a1) @ReadOnly;
  public abstract @I java.util.Map.Entry<K, V> firstEntry() @ReadOnly;
  public abstract @I java.util.Map.Entry<K, V> lastEntry() @ReadOnly;
  public abstract @I java.util.Map.Entry<K, V> pollFirstEntry() @Mutable;
  public abstract @I java.util.Map.Entry<K, V> pollLastEntry() @Mutable;
  public abstract @I java.util.NavigableMap<K, V> descendingMap() @ReadOnly;
  public abstract @I java.util.NavigableSet<K> navigableKeySet() @ReadOnly;
  public abstract @I java.util.NavigableSet<K> descendingKeySet() @ReadOnly;
  public abstract @I java.util.NavigableMap<K, V> subMap(K a1, boolean a2, K a3, boolean a4) @ReadOnly;
  public abstract @I java.util.NavigableMap<K, V> headMap(K a1, boolean a2) @ReadOnly;
  public abstract @I java.util.NavigableMap<K, V> tailMap(K a1, boolean a2) @ReadOnly;
  public abstract @I java.util.SortedMap<K, V> subMap(K a1, K a2) @ReadOnly;
  public abstract @I java.util.SortedMap<K, V> headMap(K a1) @ReadOnly;
    public abstract @I java.util.SortedMap<K, V> tailMap(K a1) @ReadOnly;
}
