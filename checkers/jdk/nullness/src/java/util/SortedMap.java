package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public interface SortedMap<K extends @NonNull Object, V extends @NonNull Object> extends java.util.Map<K, V> {
  public abstract java.util.Comparator<? super K> comparator();
  public abstract java.util.SortedMap<K, V> subMap(K a1, K a2);
  public abstract java.util.SortedMap<K, V> headMap(K a1);
  public abstract java.util.SortedMap<K, V> tailMap(K a1);
  public abstract K firstKey();
  public abstract K lastKey();
  public abstract java.util.Set<K> keySet();
  public abstract java.util.Collection<V> values();
  public abstract java.util.Set<java.util.Map.Entry<K, V>> entrySet();
}
