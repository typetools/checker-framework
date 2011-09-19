package java.util;
import checkers.javari.quals.*;

public abstract class Dictionary<K,V> {

    public Dictionary() {    }
    abstract public int size(@ReadOnly Dictionary this);
    abstract public boolean isEmpty(@ReadOnly Dictionary this);
    abstract public @ReadOnly Enumeration<K> keys(@ReadOnly Dictionary this);
    abstract public @ReadOnly Enumeration<V> elements(@ReadOnly Dictionary this);
    abstract public V get(@ReadOnly Dictionary this, @ReadOnly Object key);
    abstract public V put(K key, V value);
    abstract public V remove(@ReadOnly Object key);
}
