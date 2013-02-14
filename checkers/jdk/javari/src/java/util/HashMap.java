package java.util;
import checkers.javari.quals.*;
import java.io.*;

import com.sun.jndi.url.rmi.*;

public class HashMap<K,V>
    extends AbstractMap<K,V>
    implements Map<K,V>, Cloneable, Serializable {

    private static final long serialVersionUID = 0L;

    public HashMap(int initialCapacity, float loadFactor) { throw new RuntimeException("skeleton method"); }
    public HashMap(int initialCapacity) { throw new RuntimeException("skeleton method"); }
    public HashMap() { throw new RuntimeException("skeleton method"); }
    public HashMap(@PolyRead HashMap<K,V> this, @PolyRead Map<? extends K, ? extends V> m) { throw new RuntimeException("skeleton method"); }
    public int size() { throw new RuntimeException("skeleton method"); }
    public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
    public V get(@ReadOnly HashMap<K,V> this, @ReadOnly Object key) { throw new RuntimeException("skeleton method"); }
    public boolean containsKey(@ReadOnly HashMap<K,V> this, @ReadOnly Object key) { throw new RuntimeException("skeleton method"); }
    public V put(K key, V value) { throw new RuntimeException("skeleton method"); }
    public void putAll(@ReadOnly Map<? extends K, ? extends V> m) { throw new RuntimeException("skeleton method"); }
    public V remove(@ReadOnly Object key) { throw new RuntimeException("skeleton method"); }
    public void clear() { throw new RuntimeException("skeleton method"); }
    public boolean containsValue(@ReadOnly HashMap<K,V> this, @ReadOnly Object value) { throw new RuntimeException("skeleton method"); }
    public Object clone(@ReadOnly HashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
    public @PolyRead Set<K> keySet(@PolyRead HashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
    public @PolyRead Collection<V> values(@PolyRead HashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
    public @PolyRead Set<@PolyRead Map.Entry<K,V>> entrySet(@PolyRead HashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
}
