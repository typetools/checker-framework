package java.util;
import checkers.javari.quals.*;

public interface Map<K,V> {
    int size(@ReadOnly Map<K,V> this);
    boolean isEmpty(@ReadOnly Map<K,V> this);
    boolean containsKey(@ReadOnly Object key);
    boolean containsValue(@ReadOnly Map<K,V> this, @ReadOnly Object value);
    V get(@ReadOnly Map<K,V> this, @ReadOnly Object key);
    V put(K key, V value);
    V remove(@ReadOnly Object key);
    void putAll(@ReadOnly Map<? extends K, ? extends V> m);
    void clear();
    @PolyRead Set<K> keySet(@PolyRead Map<K,V> this);
    @PolyRead Collection<V> values(@PolyRead Map<K,V> this);
    @PolyRead Set<@PolyRead Map.Entry<K, V>> entrySet(@PolyRead Map<K,V> this);
    interface Entry<K,V> {
        K getKey(@ReadOnly Entry<K,V> this);
        V getValue(@ReadOnly Entry<K,V> this);
        V setValue(V value);
        boolean equals(@ReadOnly Entry<K,V> this, @ReadOnly Object o);
        int hashCode(@ReadOnly Entry<K,V> this);
    }

    boolean equals(@ReadOnly Map<K,V> this, @ReadOnly Object o);
    int hashCode(@ReadOnly Map<K,V> this);
}
