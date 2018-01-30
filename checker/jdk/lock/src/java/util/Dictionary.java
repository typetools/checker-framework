package java.util;

import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class Dictionary<K extends Object, V extends Object> {
  public Dictionary() { throw new RuntimeException("skeleton method"); }
   public abstract int size(@GuardSatisfied Dictionary<K,V> this);
   public abstract boolean isEmpty(@GuardSatisfied Dictionary<K,V> this);
  public abstract Enumeration<K> keys();
  public abstract Enumeration<V> elements();
  public abstract V get(@GuardSatisfied Dictionary<K,V> this, Object a1);
  public abstract V put(@GuardSatisfied Dictionary<K,V> this, K a1, V a2);
  public abstract V remove(@GuardSatisfied Dictionary<K,V> this, Object a1);
}
