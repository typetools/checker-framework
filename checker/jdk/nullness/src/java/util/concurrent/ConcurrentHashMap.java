package java.util.concurrent;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import java.util.*;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ConcurrentHashMap<K extends @NonNull Object, V extends @NonNull Object> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, java.io.Serializable 
{
     public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) { throw new RuntimeException("skeleton method"); }
     public ConcurrentHashMap(int initialCapacity, float loadFactor) { throw new RuntimeException("skeleton method"); }
     public ConcurrentHashMap(int initialCapacity) { throw new RuntimeException("skeleton method"); }
     public ConcurrentHashMap() { throw new RuntimeException("skeleton method"); }
     @Pure public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
     @Pure public int size() { throw new RuntimeException("skeleton method"); }
     @Pure public @Nullable V get(@NonNull Object key){ throw new RuntimeException("skeleton method"); }
     @Pure public boolean containsKey(@NonNull Object key) { throw new RuntimeException("skeleton method"); }
     @Pure public boolean containsValue(@NonNull Object value) { throw new RuntimeException("skeleton method"); }
     @Pure public boolean contains(@NonNull Object value) { throw new RuntimeException("skeleton method"); }
     public @Nullable V put(K key, V value) { throw new RuntimeException("skeleton method"); }
     public @Nullable V putIfAbsent(K key, V value) { throw new RuntimeException("skeleton method"); }
     public void putAll(Map<? extends K, ? extends V> m) { throw new RuntimeException("skeleton method"); }
     public @Nullable V remove(Object key) { throw new RuntimeException("skeleton method"); }
     public boolean remove(@NonNull Object key, @NonNull Object value) { throw new RuntimeException("skeleton method"); }
     public boolean replace(K key, V oldValue, V newValue) { throw new RuntimeException("skeleton method"); }
     public @Nullable V replace(K key, V value) { throw new RuntimeException("skeleton method"); }
     public void clear() { throw new RuntimeException("skeleton method"); }
     @SideEffectFree public Set<@KeyFor("this") K> keySet() { throw new RuntimeException("skeleton method"); }
     @SideEffectFree public Collection<V> values() { throw new RuntimeException("skeleton method"); }
     @SideEffectFree public Set<Map.Entry<@KeyFor("this") K, V>> entrySet() { throw new RuntimeException("skeleton method"); }
     @SideEffectFree public Enumeration<K> keys() { throw new RuntimeException("skeleton method"); }
     @SideEffectFree public Enumeration<V> elements() { throw new RuntimeException("skeleton method"); }
     @SideEffectFree public Object clone() { throw new RuntimeException("skeleton method"); }
}
