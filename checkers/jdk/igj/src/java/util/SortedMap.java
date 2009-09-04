package java.util;
import checkers.igj.quals.*;

@I
public interface SortedMap<K, V> extends @I java.util.Map<K, V> {
  public abstract @ReadOnly java.util.Comparator<? super K> comparator() @ReadOnly;
  public abstract @I java.util.SortedMap<K, V> subMap(K a1, K a2) @ReadOnly;
  public abstract @I java.util.SortedMap<K, V> headMap(K a1) @ReadOnly;
  public abstract @I java.util.SortedMap<K, V> tailMap(K a1) @ReadOnly;
  public abstract K firstKey() @ReadOnly;
  public abstract K lastKey() @ReadOnly;
  public abstract @I java.util.Set<K> keySet() @ReadOnly;
  public abstract @I java.util.Collection<V> values() @ReadOnly;
  public abstract @I java.util.Set<@I java.util.Map.Entry<K, V>> entrySet() @ReadOnly;
}
