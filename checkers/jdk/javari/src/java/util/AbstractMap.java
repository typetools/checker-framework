package java.util;
import checkers.javari.quals.*;
import java.util.Map.Entry;

import com.sun.jndi.url.rmi.*;

public abstract class AbstractMap<K,V> implements Map<K,V> {
    protected AbstractMap() { throw new RuntimeException("skeleton method"); }
    public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public boolean containsValue(@ReadOnly Object value) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public boolean containsKey(@ReadOnly Object key) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public V get(@ReadOnly Object key) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public V put(K key, V value) { throw new RuntimeException("skeleton method"); }
    public V remove(@ReadOnly Object key) { throw new RuntimeException("skeleton method"); }
    public void putAll(@ReadOnly Map<? extends K, ? extends V> m) { throw new RuntimeException("skeleton method"); }
    public void clear() { throw new RuntimeException("skeleton method"); }

    transient volatile Set<K>        keySet = null;
    transient volatile Collection<V> values = null;

    public @PolyRead Set<K> keySet() @PolyRead { throw new RuntimeException("skeleton method");}
    public @PolyRead Collection<V> values() @PolyRead { throw new RuntimeException("skeleton method"); }
    public abstract @PolyRead Set<@PolyRead Entry<K,V>> entrySet() @PolyRead;
    public boolean equals(@ReadOnly Object o) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
    protected Object clone() @ReadOnly throws CloneNotSupportedException  { throw new RuntimeException("skeleton method"); }

    public static class SimpleEntry<K,V>
    implements Entry<K,V>, java.io.Serializable {
        public SimpleEntry(K key, V value) { throw new RuntimeException("skeleton method"); }
        public SimpleEntry(@ReadOnly Entry<? extends K, ? extends V> entry) { throw new RuntimeException("skeleton method"); }
        public K getKey() @ReadOnly { throw new RuntimeException("skeleton method"); }
        public V getValue() @ReadOnly { throw new RuntimeException("skeleton method"); }
        public V setValue(V value) { throw new RuntimeException("skeleton method"); }
        public boolean equals(@ReadOnly Object o) @ReadOnly { throw new RuntimeException("skeleton method"); }
        public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
        public String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
    }

    public static @ReadOnly class SimpleImmutableEntry<K,V>
    implements Entry<K,V>, java.io.Serializable {
        public SimpleImmutableEntry(K key, V value) { throw new RuntimeException("skeleton method"); }
        public SimpleImmutableEntry(@ReadOnly Entry<? extends K, ? extends V> entry) { throw new RuntimeException("skeleton method"); }
        public K getKey() { throw new RuntimeException("skeleton method"); }
        public V getValue() { throw new RuntimeException("skeleton method"); }
        public V setValue(V value) { throw new RuntimeException("skeleton method"); }
        public boolean equals(@ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
        public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
        public String toString() { throw new RuntimeException("skeleton method"); }

    }
}
