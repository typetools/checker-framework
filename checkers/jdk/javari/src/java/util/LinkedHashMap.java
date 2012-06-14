package java.util;
import checkers.javari.quals.*;
import java.io.*;

public class LinkedHashMap<K,V>
    extends HashMap<K,V>
    implements Map<K,V>
{
    private static final long serialVersionUID = 0L;
    public LinkedHashMap(int initialCapacity, float loadFactor) { throw new RuntimeException("skeleton method"); }
    public LinkedHashMap(int initialCapacity) { throw new RuntimeException("skeleton method"); }
    public LinkedHashMap() { throw new RuntimeException("skeleton method"); }
    public LinkedHashMap(@ReadOnly Map<? extends K, ? extends V> m) { throw new RuntimeException("skeleton method"); }
    public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) { throw new RuntimeException("skeleton method"); }
    public boolean containsValue(@ReadOnly LinkedHashMap<K,V> this, @ReadOnly Object value) { throw new RuntimeException("skeleton method"); }
    public V get(@ReadOnly LinkedHashMap<K,V> this, @ReadOnly Object key) { throw new RuntimeException("skeleton method"); }
    public void clear() { throw new RuntimeException("skeleton method"); }
    protected boolean removeEldestEntry(@ReadOnly Map.Entry<K,V> eldest) { throw new RuntimeException("skeleton method"); }
}
