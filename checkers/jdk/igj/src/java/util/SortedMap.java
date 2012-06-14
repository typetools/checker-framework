package java.util;
import checkers.igj.quals.*;

@I
public interface SortedMap<K, V> extends @I Map<K, V> {
  public abstract @ReadOnly Comparator<? super K> comparator() @ReadOnly;
  public abstract @I SortedMap<K, V> subMap(K a1, K a2) @ReadOnly;
  public abstract @I SortedMap<K, V> headMap(K a1) @ReadOnly;
  public abstract @I SortedMap<K, V> tailMap(K a1) @ReadOnly;
  public abstract K firstKey() @ReadOnly;
  public abstract K lastKey() @ReadOnly;
  public abstract @I Set<K> keySet() @ReadOnly;
  public abstract @I Collection<V> values() @ReadOnly;
  public abstract @I Set<@I Map.Entry<K, V>> entrySet() @ReadOnly;
}
