package java.util;
import checkers.igj.quals.*;

@I
public abstract class Dictionary<K, V> {
  public Dictionary(@AssignsFields Dictionary<K, V> this) { throw new RuntimeException("skeleton method"); }
  public abstract int size(@ReadOnly Dictionary<K, V> this);
  public abstract boolean isEmpty(@ReadOnly Dictionary<K, V> this);
  public abstract Enumeration<K> keys(@ReadOnly Dictionary<K, V> this);
  public abstract Enumeration<V> elements(@ReadOnly Dictionary<K, V> this);
  public abstract V get(@ReadOnly Dictionary<K, V> this, Object a1) ;
  public abstract V put(@Mutable Dictionary<K, V> this, K a1, V a2);
  public abstract V remove(@Mutable Dictionary<K, V> this, Object a1);
}
