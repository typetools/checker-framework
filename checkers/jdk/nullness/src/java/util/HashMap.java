package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class HashMap<K extends @Nullable Object, V extends @Nullable Object> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public HashMap(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public HashMap(int a1) { throw new RuntimeException("skeleton method"); }
  public HashMap() { throw new RuntimeException("skeleton method"); }
  public HashMap(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public int size() { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public @Pure @Nullable V get(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public @Pure boolean containsKey(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable V put(K a1, V a2) { throw new RuntimeException("skeleton method"); }
  public void putAll(Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable V remove(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public Set<@KeyFor("this") K> keySet() { throw new RuntimeException("skeleton method"); }
  public Collection<V> values() { throw new RuntimeException("skeleton method"); }
  public Set<Map.Entry<@KeyFor("this") K, V>> entrySet() { throw new RuntimeException("skeleton method"); }
  public Object clone() { throw new RuntimeException("skeleton method"); }
}
