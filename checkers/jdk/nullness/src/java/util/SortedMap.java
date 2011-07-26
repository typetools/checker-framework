package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public interface SortedMap<K extends @Nullable Object, V extends @Nullable Object> extends Map<K, V> {
  public abstract Comparator<? super K> comparator();
  public abstract SortedMap<K, V> subMap(K a1, K a2);
  public abstract SortedMap<K, V> headMap(K a1);
  public abstract SortedMap<K, V> tailMap(K a1);
  public abstract K firstKey();
  public abstract K lastKey();
  public abstract Set<@KeyFor("this") K> keySet();
  public abstract Collection<V> values();
  public abstract Set<Map.Entry<@KeyFor("this") K, V>> entrySet();
}
