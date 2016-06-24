package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;

// Subclasses of this interface/class may opt to prohibit null elements
public interface NavigableMap<K extends @Nullable Object, V extends @Nullable Object> extends SortedMap<K, V> {
  public abstract Map. @Nullable Entry<K, V> lowerEntry(K a1);
  public abstract @Nullable K lowerKey(K a1);
  public abstract Map. @Nullable Entry<K, V> floorEntry(K a1);
  public abstract @Nullable K floorKey(K a1);
  public abstract Map. @Nullable Entry<K, V> ceilingEntry(K a1);
  public abstract @Nullable K ceilingKey(K a1);
  public abstract Map. @Nullable Entry<K, V> higherEntry(K a1);
  public abstract @Nullable K higherKey(K a1);
  public abstract Map. @Nullable Entry<K, V> firstEntry();
  public abstract Map. @Nullable Entry<K, V> lastEntry();
  public abstract Map. @Nullable Entry<K, V> pollFirstEntry();
  public abstract Map. @Nullable Entry<K, V> pollLastEntry();
  public abstract NavigableMap<K, V> descendingMap();
  public abstract NavigableSet<@KeyFor("this") K> navigableKeySet();
  public abstract NavigableSet<@KeyFor("this") K> descendingKeySet();
  public abstract NavigableMap<K, V> subMap(K a1, boolean a2, K a3, boolean a4);
  public abstract NavigableMap<K, V> headMap(K a1, boolean a2);
  public abstract NavigableMap<K, V> tailMap(K a1, boolean a2);
  public abstract SortedMap<K, V> subMap(K a1, K a2);
  public abstract SortedMap<K, V> headMap(K a1);
  public abstract SortedMap<K, V> tailMap(K a1);

  @EnsuresNonNullIf(expression={"firstEntry()", "pollFirstEntry()", "lastEntry()", "pollLastEntry()"}, result=false)
  @Pure public abstract boolean isEmpty();
}
