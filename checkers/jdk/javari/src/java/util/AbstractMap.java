package java.util;
import checkers.javari.quals.*;
import java.util.Map.Entry;

import com.sun.jndi.url.rmi.*;

public abstract class AbstractMap<K,V> implements Map<K,V> {
    protected AbstractMap() { throw new RuntimeException("skeleton method"); }
    public int size(@ReadOnly AbstractMap this) { throw new RuntimeException("skeleton method"); }
    public boolean isEmpty(@ReadOnly AbstractMap this) { throw new RuntimeException("skeleton method"); }
    public boolean containsValue(@ReadOnly AbstractMap this, @ReadOnly Object value) { throw new RuntimeException("skeleton method"); }
    public boolean containsKey(@ReadOnly AbstractMap this, @ReadOnly Object key) { throw new RuntimeException("skeleton method"); }
    public V get(@ReadOnly AbstractMap this, @ReadOnly Object key) { throw new RuntimeException("skeleton method"); }
    public V put(K key, V value) { throw new RuntimeException("skeleton method"); }
    public V remove(@ReadOnly Object key) { throw new RuntimeException("skeleton method"); }
    public void putAll(@ReadOnly Map<? extends K, ? extends V> m) { throw new RuntimeException("skeleton method"); }
    public void clear() { throw new RuntimeException("skeleton method"); }

    transient volatile Set<K>        keySet = null;
    transient volatile Collection<V> values = null;

    public @PolyRead Set<K> keySet(@PolyRead AbstractMap this) { throw new RuntimeException("skeleton method");}
    public @PolyRead Collection<V> values(@PolyRead AbstractMap this) { throw new RuntimeException("skeleton method"); }
    public abstract @PolyRead Set<@PolyRead Entry<K,V>> entrySet(@PolyRead AbstractMap this);
    public boolean equals(@ReadOnly AbstractMap this, @ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
    public int hashCode(@ReadOnly AbstractMap this) { throw new RuntimeException("skeleton method"); }
    public String toString(@ReadOnly AbstractMap this) { throw new RuntimeException("skeleton method"); }
    protected Object clone(@ReadOnly AbstractMap this) throws CloneNotSupportedException  { throw new RuntimeException("skeleton method"); }

    public static class SimpleEntry<K,V>
    implements Entry<K,V>, java.io.Serializable {
        private static final long serialVersionUID = 0L;
        public SimpleEntry(K key, V value) { throw new RuntimeException("skeleton method"); }
        public SimpleEntry(@ReadOnly Entry<? extends K, ? extends V> entry) { throw new RuntimeException("skeleton method"); }
        public K getKey(@ReadOnly SimpleEntry this) { throw new RuntimeException("skeleton method"); }
        public V getValue(@ReadOnly SimpleEntry this) { throw new RuntimeException("skeleton method"); }
        public V setValue(V value) { throw new RuntimeException("skeleton method"); }
        public boolean equals(@ReadOnly SimpleEntry this, @ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
        public int hashCode(@ReadOnly SimpleEntry this) { throw new RuntimeException("skeleton method"); }
        public String toString(@ReadOnly SimpleEntry this) { throw new RuntimeException("skeleton method"); }
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
        public int hashCode(@ReadOnly SimpleImmutableEntry this) { throw new RuntimeException("skeleton method"); }
        public String toString() { throw new RuntimeException("skeleton method"); }

    }
}
