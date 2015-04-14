package java.util;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.Pure;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LinkedHashMap<K extends @Nullable Object, V extends @Nullable Object> extends HashMap<K, V> implements Map<K, V> {
  private static final long serialVersionUID = 0;
  public LinkedHashMap(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap(int a1) { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap() { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap(int a1, float a2, boolean a3) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean containsValue(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public @Nullable V get(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  protected boolean removeEldestEntry(Map.Entry<K, V> entry) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Set<@KeyFor("this") K> keySet() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Set<Map.Entry<@KeyFor("this") K, V>> entrySet() { throw new RuntimeException("skeleton method"); }
}
