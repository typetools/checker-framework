package java.util;
import checkers.javari.quals.*;

public interface Map<K,V> {
    int size() @ReadOnly;
    boolean isEmpty() @ReadOnly;
    boolean containsKey(@ReadOnly Object key);
    boolean containsValue(@ReadOnly Object value) @ReadOnly;
    V get(@ReadOnly Object key) @ReadOnly;
    V put(K key, V value);
    V remove(@ReadOnly Object key);
    void putAll(@ReadOnly Map<? extends K, ? extends V> m);
    void clear();
    @PolyRead Set<K> keySet() @PolyRead;
    @PolyRead Collection<V> values() @PolyRead;
    @PolyRead Set<@PolyRead Map.Entry<K, V>> entrySet() @PolyRead;
    interface Entry<K,V> {
        K getKey() @ReadOnly;
        V getValue() @ReadOnly;
        V setValue(V value);
        boolean equals(@ReadOnly Object o) @ReadOnly;
        int hashCode() @ReadOnly;
    }

    boolean equals(@ReadOnly Object o) @ReadOnly;
    int hashCode() @ReadOnly;
}
