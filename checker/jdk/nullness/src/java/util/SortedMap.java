package java.util;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.*;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;

// Subclasses of this interface/class may opt to prohibit null elements
public interface SortedMap<K extends @Nullable Object, V extends @Nullable Object> extends Map<K, V> {
  @SideEffectFree public abstract Comparator<? super K> comparator();
  @SideEffectFree public abstract SortedMap<K, V> subMap(K a1, K a2);
  @SideEffectFree public abstract SortedMap<K, V> headMap(K a1);
  @SideEffectFree public abstract SortedMap<K, V> tailMap(K a1);
  @SideEffectFree public abstract K firstKey();
  @SideEffectFree public abstract K lastKey();
  @SideEffectFree public abstract Set<@KeyFor("this") K> keySet();
  @SideEffectFree public abstract Collection<V> values();
  @SideEffectFree public abstract Set<Map.Entry<@KeyFor("this") K, V>> entrySet();
}
