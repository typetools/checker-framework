package java.util;
import org.checkerframework.checker.igj.qual.*;

@I
public interface SortedMap<K, V> extends @I Map<K, V> {
  public abstract @ReadOnly Comparator<? super K> comparator(@ReadOnly SortedMap<K, V> this);
  public abstract @I SortedMap<K, V> subMap(@ReadOnly SortedMap<K, V> this, K a1, K a2);
  public abstract @I SortedMap<K, V> headMap(@ReadOnly SortedMap<K, V> this, K a1);
  public abstract @I SortedMap<K, V> tailMap(@ReadOnly SortedMap<K, V> this, K a1);
  public abstract K firstKey(@ReadOnly SortedMap<K, V> this);
  public abstract K lastKey(@ReadOnly SortedMap<K, V> this);
  public abstract @I Set<K> keySet(@ReadOnly SortedMap<K, V> this);
  public abstract @I Collection<V> values(@ReadOnly SortedMap<K, V> this);
  public abstract @I Set<Map. @I Entry<K, V>> entrySet(@ReadOnly SortedMap<K, V> this);
}
