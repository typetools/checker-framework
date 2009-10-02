package java.util;
import checkers.javari.quals.*;
import java.io.*;

public class LinkedHashMap<K,V>
    extends HashMap<K,V>
    implements Map<K,V>
{
    public LinkedHashMap(int initialCapacity, float loadFactor) { throw new RuntimeException("skeleton method"); }
    public LinkedHashMap(int initialCapacity) { throw new RuntimeException("skeleton method"); }
    public LinkedHashMap() { throw new RuntimeException("skeleton method"); }
    public LinkedHashMap(@ReadOnly Map<? extends K, ? extends V> m) { throw new RuntimeException("skeleton method"); }
    public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) { throw new RuntimeException("skeleton method"); }
    public boolean containsValue(@ReadOnly Object value) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public V get(@ReadOnly Object key) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public void clear() { throw new RuntimeException("skeleton method"); }
    protected boolean removeEldestEntry(@ReadOnly Map.Entry<K,V> eldest) { throw new RuntimeException("skeleton method"); }
}
