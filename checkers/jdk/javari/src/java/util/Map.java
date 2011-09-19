package java.util;
import checkers.javari.quals.*;

public interface Map<K,V> {
    int size(@ReadOnly Map this);
    boolean isEmpty(@ReadOnly Map this);
    boolean containsKey(@ReadOnly Object key);
    boolean containsValue(@ReadOnly Map this, @ReadOnly Object value);
    V get(@ReadOnly Map this, @ReadOnly Object key);
    V put(K key, V value);
    V remove(@ReadOnly Object key);
    void putAll(@ReadOnly Map<? extends K, ? extends V> m);
    void clear();
    @PolyRead Set<K> keySet(@PolyRead Map this);
    @PolyRead Collection<V> values(@PolyRead Map this);
    @PolyRead Set<@PolyRead Map.Entry<K, V>> entrySet(@PolyRead Map this);
    interface Entry<K,V> {
        K getKey(@ReadOnly Entry this);
        V getValue(@ReadOnly Entry this);
        V setValue(V value);
        boolean equals(@ReadOnly Entry this, @ReadOnly Object o);
        int hashCode(@ReadOnly Entry this);
    }

    boolean equals(@ReadOnly Entry this, @ReadOnly Object o);
    int hashCode(@ReadOnly Entry this);
}
