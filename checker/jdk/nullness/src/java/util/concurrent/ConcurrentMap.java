
package java.util.concurrent;



import java.util.Map;


public interface ConcurrentMap<K extends @NonNull Object, V extends @NonNull Object> extends Map<K, V> {
    
    @Nullable V putIfAbsent(K key, V value);

   
    boolean remove(Object key, Object value);

  
    boolean replace(K key, V oldValue, V newValue);

    
    @Nullable V replace(K key, V value);
   
}
