package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class LinkedHashMap<K, V> extends java.util.HashMap<K, V> implements java.util.Map<K, V> {
  private static final long serialVersionUID = 0;
  public LinkedHashMap(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap(int a1) { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap() { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap(java.util.Map<? extends K, ? extends V> a1) { throw new RuntimeException("skeleton method"); }
  public LinkedHashMap(int a1, float a2, boolean a3) { throw new RuntimeException("skeleton method"); }
  public boolean containsValue(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public @Pure V get(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  protected boolean removeEldestEntry(java.util.Map.Entry<K, V> entry) { throw new RuntimeException("skeleton method"); }
}
