package java.util;
import checkers.javari.quals.*;
import java.util.Map.Entry;

import com.sun.jndi.url.rmi.*;

public abstract class AbstractMap<K,V> implements Map<K,V> {
    protected AbstractMap() { throw new RuntimeException("skeleton method"); }
    public int size(@ReadOnly AbstractMap<K,V> this) { throw new RuntimeException("skeleton method"); }
    public boolean isEmpty(@ReadOnly AbstractMap<K,V> this) { throw new RuntimeException("skeleton method"); }
    public boolean containsValue(@ReadOnly AbstractMap<K,V> this, @ReadOnly Object value) { throw new RuntimeException("skeleton method"); }
    public boolean containsKey(@ReadOnly AbstractMap<K,V> this, @ReadOnly Object key) { throw new RuntimeException("skeleton method"); }
    public V get(@ReadOnly AbstractMap<K,V> this, @ReadOnly Object key) { throw new RuntimeException("skeleton method"); }
    public V put(K key, V value) { throw new RuntimeException("skeleton method"); }
    public V remove(@ReadOnly Object key) { throw new RuntimeException("skeleton method"); }
    public void putAll(@ReadOnly Map<? extends K, ? extends V> m) { throw new RuntimeException("skeleton method"); }
    public void clear() { throw new RuntimeException("skeleton method"); }

    transient volatile Set<K>        keySet = null;
    transient volatile Collection<V> values = null;

    public @PolyRead Set<K> keySet(@PolyRead AbstractMap<K,V> this) { throw new RuntimeException("skeleton method");}
    public @PolyRead Collection<V> values(@PolyRead AbstractMap<K,V> this) { throw new RuntimeException("skeleton method"); }
    public abstract @PolyRead Set<@PolyRead Entry<K,V>> entrySet(@PolyRead AbstractMap<K,V> this);
    public boolean equals(@ReadOnly AbstractMap<K,V> this, @ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
    public int hashCode(@ReadOnly AbstractMap<K,V> this) { throw new RuntimeException("skeleton method"); }
    public String toString(@ReadOnly AbstractMap<K,V> this) { throw new RuntimeException("skeleton method"); }
    protected Object clone(@ReadOnly AbstractMap<K,V> this) throws CloneNotSupportedException  { throw new RuntimeException("skeleton method"); }

    public static class SimpleEntry<K,V>
    implements Entry<K,V>, java.io.Serializable {
        private static final long serialVersionUID = 0L;
        public SimpleEntry(K key, V value) { throw new RuntimeException("skeleton method"); }
        public SimpleEntry(@ReadOnly Entry<? extends K, ? extends V> entry) { throw new RuntimeException("skeleton method"); }
        public K getKey(@ReadOnly SimpleEntry<K,V> this) { throw new RuntimeException("skeleton method"); }
        public V getValue(@ReadOnly SimpleEntry<K,V> this) { throw new RuntimeException("skeleton method"); }
        public V setValue(V value) { throw new RuntimeException("skeleton method"); }
        public boolean equals(@ReadOnly SimpleEntry<K,V> this, @ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
        public int hashCode(@ReadOnly SimpleEntry<K,V> this) { throw new RuntimeException("skeleton method"); }
        public String toString(@ReadOnly SimpleEntry<K,V> this) { throw new RuntimeException("skeleton method"); }
    }

    public static @ReadOnly class SimpleImmutableEntry<K,V>
    implements Entry<K,V>, java.io.Serializable {
        private static final long serialVersionUID = 0L;
        public SimpleImmutableEntry(K key, V value) { throw new RuntimeException("skeleton method"); }
        public SimpleImmutableEntry(@ReadOnly Entry<? extends K, ? extends V> entry) { throw new RuntimeException("skeleton method"); }
        public K getKey() { throw new RuntimeException("skeleton method"); }
        public V getValue() { throw new RuntimeException("skeleton method"); }
        public V setValue(V value) { throw new RuntimeException("skeleton method"); }
        public boolean equals(@ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
        public int hashCode(@ReadOnly SimpleImmutableEntry<K,V> this) { throw new RuntimeException("skeleton method"); }
        public String toString() { throw new RuntimeException("skeleton method"); }

    }
}
