package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements.
// We write "extends @NonNull Object" for emphasis even though it's the default.
public interface Map<K extends @NonNull Object, V extends @NonNull Object> {
  public static interface Entry<K, V> {
    public abstract K getKey();
    public abstract V getValue();
    public abstract V setValue(V a1);
    public abstract boolean equals(@Nullable java.lang.Object a1);
    public abstract int hashCode();
  }
  public abstract int size();
  public abstract boolean isEmpty();
  public abstract boolean containsKey(@Nullable java.lang.Object a1);
  public abstract boolean containsValue(@Nullable java.lang.Object a1);
  // The parameter is not nullable, because implementations of Map.get and
  // Map.put are specifically premitted to throw NullPointerException if
  // any of the arguments is a null).  And some implementations do not
  // permit nulls (sorted queues PriorityQueue, Hashtable, most concurrent
  // collections).  Some other implementation do accept nulls and aro so
  // annotatied (see ArrayList, LinkedList, HashMap).
  public abstract @Nullable V get(@Nullable java.lang.Object a1);
  public abstract @Nullable V put(K a1, V a2);
  public abstract @Nullable V remove(@Nullable java.lang.Object a1);
  public abstract void putAll(java.util.Map<? extends K, ? extends V> a1);
  public abstract void clear();
  public abstract java.util.Set<K> keySet();
  public abstract java.util.Collection<V> values();
  public abstract java.util.Set<java.util.Map.Entry<K, V>> entrySet();
  public abstract boolean equals(@Nullable java.lang.Object a1);
  public abstract int hashCode();
}
