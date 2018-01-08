package java.util.concurrent;
import java.util.Map;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import java.util.*;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;


public interface ConcurrentMap<K extends @NonNull Object, V extends @NonNull Object> extends Map<K, V>
{
     @Pure public boolean isEmpty();
     @Pure public int size();
     @Pure public @Nullable V get(Object key);
     @Pure public boolean containsKey(Object key);
     @Pure public boolean containsValue(Object value);
     @Pure public boolean contains(Object value);
     public @Nullable V put(K key, V value);
     public @Nullable V putIfAbsent(K key, V value);
     public void putAll(Map<? extends K, ? extends V> m);
     public @Nullable V remove(Object key);
     public boolean remove(Object key, Object value);
     public boolean replace(K key, V oldValue, V newValue);
     public @Nullable V replace(K key, V value);
     public void clear();
     @SideEffectFree public Set<@KeyFor("this") K> keySet();
     @SideEffectFree public Collection<V> values();
     @SideEffectFree public Set<Map.Entry<@KeyFor("this") K, V>> entrySet();
     @SideEffectFree public Enumeration<K> keys();
     @SideEffectFree public Enumeration<V> elements();
     @SideEffectFree public Object clone();
}
