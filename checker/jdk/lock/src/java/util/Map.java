package java.util;
import org.checkerframework.dataflow.qual.*;

import org.checkerframework.checker.nullness.qual.Covariant;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit null elements
public interface Map<K extends Object, V extends Object> {
  public static interface Entry<K extends Object, V extends Object> {
     public abstract K getKey(@GuardSatisfied Entry<K,V> this);
     public abstract V getValue(@GuardSatisfied Entry<K,V> this);
    public abstract V setValue(V a1);
     public abstract boolean equals(@GuardSatisfied Entry<K,V> this, @GuardSatisfied Object a1);
     public abstract int hashCode(@GuardSatisfied Entry<K,V> this);
  }
   public abstract int size(@GuardSatisfied Map<K,V> this);
   public abstract boolean isEmpty(@GuardSatisfied Map<K,V> this);
   public abstract boolean containsKey(@GuardSatisfied Map<K,V> this, @GuardSatisfied Object a1);
   public abstract boolean containsValue(@GuardSatisfied Map<K,V> this, @GuardSatisfied Object a1);
  // The parameter is not nullable, because implementations of Map.get and
  // Map.put are specifically permitted to throw NullPointerException if
  // any of the arguments is a null).  And some implementations do not
  // permit nulls (sorted queues PriorityQueue, Hashtable, most concurrent
  // collections).  Some other implementation do accept nulls and are so
  // annotatied (see ArrayList, LinkedList, HashMap).
   public abstract V get(@GuardSatisfied Map<K,V> this, @GuardSatisfied Object a1);
  @ReleasesNoLocks public abstract V put(@GuardSatisfied Map<K,V> this, K a1, V a2);
  public abstract V remove(Object a1);
  public abstract void putAll(Map<? extends K, ? extends V> a1);
  public abstract void clear();
   public abstract Set<K> keySet(@GuardSatisfied Map<K,V> this);
   public abstract Collection<V> values(@GuardSatisfied Map<K,V> this);
   public abstract Set<Map.Entry<K,V>> entrySet(@GuardSatisfied Map<K,V> this);
   public abstract boolean equals(@GuardSatisfied Map<K,V> this, @GuardSatisfied Object a1);
   public abstract int hashCode(@GuardSatisfied Map<K,V> this);
}
