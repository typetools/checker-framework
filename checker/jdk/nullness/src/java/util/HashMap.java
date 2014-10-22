package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;

public class HashMap<K extends @Nullable Object, V extends @Nullable Object> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public HashMap(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public HashMap(int a1) { throw new RuntimeException("skeleton method"); }
  public HashMap() { throw new RuntimeException("skeleton method"); }
  public HashMap(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int size() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  @Pure public @Nullable V get(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean containsKey(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable V put(K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public void putAll(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable V remove(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean containsValue(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Set<@KeyFor("this") K> keySet() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Collection<V> values() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Set<Map.Entry<@KeyFor("this") K, V>> entrySet() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Object clone() { throw new RuntimeException("skeleton method"); }
}
