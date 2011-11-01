package java.util;
import checkers.javari.quals.*;

public abstract class Dictionary<K,V> {

    public Dictionary() {    }
    abstract public int size(@ReadOnly Dictionary<K,V> this);
    abstract public boolean isEmpty(@ReadOnly Dictionary<K,V> this);
    abstract public @ReadOnly Enumeration<K> keys(@ReadOnly Dictionary<K,V> this);
    abstract public @ReadOnly Enumeration<V> elements(@ReadOnly Dictionary<K,V> this);
    abstract public V get(@ReadOnly Dictionary<K,V> this, @ReadOnly Object key);
    abstract public V put(K key, V value);
    abstract public V remove(@ReadOnly Object key);
}
