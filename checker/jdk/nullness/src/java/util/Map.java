package java.util;

import org.checkerframework.dataflow.qual.*;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.Covariant;

// Subclasses of this interface/class may opt to prohibit null elements
public interface Map<K extends @Nullable Object, V extends @Nullable Object> {
  @Covariant(0)
  public static interface Entry<K extends @Nullable Object, V extends @Nullable Object> {
    @Pure public abstract K getKey();
    @Pure public abstract V getValue();
    public abstract V setValue(V a1);
    @Pure public abstract boolean equals(@Nullable Object a1);
    @Pure public abstract int hashCode();
  }
  @Pure public abstract int size();
  @Pure public abstract boolean isEmpty();
  @Pure public abstract boolean containsKey(@Nullable Object a1);
  @Pure public abstract boolean containsValue(@Nullable Object a1);
  // The parameter is not nullable, because implementations of Map.get and
  // Map.put are specifically permitted to throw NullPointerException if
  // any of the arguments is a null).  And some implementations do not
  // permit nulls (sorted queues PriorityQueue, Hashtable, most concurrent
  // collections).  Some other implementation do accept nulls and are so
  // annotatied (see ArrayList, LinkedList, HashMap).
  @Pure public abstract @Nullable V get(@Nullable Object a1);
  public abstract @Nullable V put(K a1, V a2);
  public abstract @Nullable V remove(@Nullable Object a1);
  public abstract void putAll(Map<? extends K, ? extends V> a1);
  public abstract void clear();
  @SideEffectFree public abstract Set<@KeyFor("this") K> keySet();
  @SideEffectFree public abstract Collection<V> values();
  @SideEffectFree public abstract Set<Map.Entry<@KeyFor("this") K, V>> entrySet();
  @Pure public abstract boolean equals(@Nullable Object a1);
  @Pure public abstract int hashCode();
}
