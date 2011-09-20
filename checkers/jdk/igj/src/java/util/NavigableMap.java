package java.util;
import checkers.igj.quals.*;

@I
public interface NavigableMap<K, V> extends @I SortedMap<K, V> {
  public abstract @I Map.Entry<K, V> lowerEntry(@ReadOnly NavigableMap<K, V> this, K a1);
  public abstract K lowerKey(@ReadOnly NavigableMap<K, V> this, K a1);
  public abstract @I Map.Entry<K, V> floorEntry(@ReadOnly NavigableMap<K, V> this, K a1);
  public abstract K floorKey(@ReadOnly NavigableMap<K, V> this, K a1);
  public abstract @I Map.Entry<K, V> ceilingEntry(@ReadOnly NavigableMap<K, V> this, K a1);
  public abstract K ceilingKey(@ReadOnly NavigableMap<K, V> this, K a1);
  public abstract @I Map.Entry<K, V> higherEntry(@ReadOnly NavigableMap<K, V> this, K a1);
  public abstract K higherKey(@ReadOnly NavigableMap<K, V> this, K a1);
  public abstract @I Map.Entry<K, V> firstEntry(@ReadOnly NavigableMap<K, V> this);
  public abstract @I Map.Entry<K, V> lastEntry(@ReadOnly NavigableMap<K, V> this);
  public abstract @I Map.Entry<K, V> pollFirstEntry(@Mutable NavigableMap<K, V> this);
  public abstract @I Map.Entry<K, V> pollLastEntry(@Mutable NavigableMap<K, V> this);
  public abstract @I NavigableMap<K, V> descendingMap(@ReadOnly NavigableMap<K, V> this);
  public abstract @I NavigableSet<K> navigableKeySet(@ReadOnly NavigableMap<K, V> this);
  public abstract @I NavigableSet<K> descendingKeySet(@ReadOnly NavigableMap<K, V> this);
  public abstract @I NavigableMap<K, V> subMap(@ReadOnly NavigableMap<K, V> this, K a1, boolean a2, K a3, boolean a4);
  public abstract @I NavigableMap<K, V> headMap(@ReadOnly NavigableMap<K, V> this, K a1, boolean a2);
  public abstract @I NavigableMap<K, V> tailMap(@ReadOnly NavigableMap<K, V> this, K a1, boolean a2);
  public abstract @I SortedMap<K, V> subMap(@ReadOnly NavigableMap<K, V> this, K a1, K a2);
  public abstract @I SortedMap<K, V> headMap(@ReadOnly NavigableMap<K, V> this, K a1);
    public abstract @I SortedMap<K, V> tailMap(@ReadOnly NavigableMap<K, V> this, K a1);
}
