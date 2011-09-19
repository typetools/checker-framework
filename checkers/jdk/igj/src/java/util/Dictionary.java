package java.util;
import checkers.igj.quals.*;

@I
public abstract class Dictionary<K, V> {
  public Dictionary(@AssignsFields Dictionary this) { throw new RuntimeException("skeleton method"); }
  public abstract int size(@ReadOnly Dictionary this);
  public abstract boolean isEmpty(@ReadOnly Dictionary this);
  public abstract Enumeration<K> keys(@ReadOnly Dictionary this);
  public abstract Enumeration<V> elements(@ReadOnly Dictionary this);
  public abstract V get(@ReadOnly Dictionary this, Object a1) ;
  public abstract V put(@Mutable Dictionary this, K a1, V a2);
  public abstract V remove(@Mutable Dictionary this, Object a1);
}
