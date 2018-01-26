package java.util;


import org.checkerframework.checker.lock.qual.*;

public class LinkedHashMap<K extends Object, V extends Object> extends HashMap<K, V> implements Map<K, V> {
  private static final long serialVersionUID = 0;
  public LinkedHashMap(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap(int a1) { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap() { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap(int a1, float a2, boolean a3) { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@GuardSatisfied LinkedHashMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public V get(@GuardSatisfied LinkedHashMap<K,V> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@GuardSatisfied LinkedHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
  protected boolean removeEldestEntry(@GuardSatisfied LinkedHashMap<K,V> this, Map.Entry<K, V> entry) { throw new RuntimeException("skeleton method"); }
   public Set<K> keySet() { throw new RuntimeException("skeleton method"); }
   public Set<Map.Entry<K,V>> entrySet(@GuardSatisfied LinkedHashMap<K,V> this) { throw new RuntimeException("skeleton method"); }
}
