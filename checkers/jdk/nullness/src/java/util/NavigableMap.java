package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public interface NavigableMap<K extends @Nullable Object, V extends @Nullable Object> extends SortedMap<K, V> {
  public abstract @Nullable Map.Entry<K, V> lowerEntry(K a1);
  public abstract @Nullable K lowerKey(K a1);
  public abstract @Nullable Map.Entry<K, V> floorEntry(K a1);
  public abstract @Nullable K floorKey(K a1);
  public abstract @Nullable Map.Entry<K, V> ceilingEntry(K a1);
  public abstract @Nullable K ceilingKey(K a1);
  public abstract @Nullable Map.Entry<K, V> higherEntry(K a1);
  public abstract @Nullable K higherKey(K a1);
  public abstract @Nullable Map.Entry<K, V> firstEntry();
  public abstract @Nullable Map.Entry<K, V> lastEntry();
  public abstract @Nullable Map.Entry<K, V> pollFirstEntry();
  public abstract @Nullable Map.Entry<K, V> pollLastEntry();
  public abstract NavigableMap<K, V> descendingMap();
  public abstract NavigableSet<@KeyFor("this") K> navigableKeySet();
  public abstract NavigableSet<@KeyFor("this") K> descendingKeySet();
  public abstract NavigableMap<K, V> subMap(K a1, boolean a2, K a3, boolean a4);
  public abstract NavigableMap<K, V> headMap(K a1, boolean a2);
  public abstract NavigableMap<K, V> tailMap(K a1, boolean a2);
  public abstract SortedMap<K, V> subMap(K a1, K a2);
  public abstract SortedMap<K, V> headMap(K a1);
  public abstract SortedMap<K, V> tailMap(K a1);

  @AssertNonNullIfFalse({"firstEntry()", "pollFirstEntry()", "lastEntry()", "pollLastEntry()"})
  public abstract boolean isEmpty();
}
