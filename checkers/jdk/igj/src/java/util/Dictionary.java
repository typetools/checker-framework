package java.util;
import checkers.igj.quals.*;

@I
public abstract class Dictionary<K, V> {
  public Dictionary() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public abstract int size() @ReadOnly;
  public abstract boolean isEmpty() @ReadOnly;
  public abstract Enumeration<K> keys() @ReadOnly;
  public abstract Enumeration<V> elements() @ReadOnly;
  public abstract V get(Object a1) @ReadOnly ;
  public abstract V put(K a1, V a2) @Mutable;
  public abstract V remove(Object a1) @Mutable;
}
