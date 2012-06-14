package java.util;
import checkers.javari.quals.*;

public abstract class Dictionary<K,V> {

    public Dictionary() {    }
    abstract public int size() @ReadOnly;
    abstract public boolean isEmpty() @ReadOnly;
    abstract public @ReadOnly Enumeration<K> keys() @ReadOnly;
    abstract public @ReadOnly Enumeration<V> elements() @ReadOnly;
    abstract public V get(@ReadOnly Object key) @ReadOnly;
    abstract public V put(K key, V value);
    abstract public V remove(@ReadOnly Object key);
}
