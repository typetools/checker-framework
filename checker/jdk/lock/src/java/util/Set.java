package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.checker.nullness.qual.PolyNull;

// Subclasses of this interface/class may opt to prohibit null elements
public interface Set<E extends Object> extends Collection<E> {
  @Pure public abstract int size(@GuardSatisfied Set<E> this);
  @Pure public abstract boolean isEmpty(@GuardSatisfied Set<E> this);
  @Pure public abstract boolean contains(@GuardSatisfied Set<E> this,Object a1);
  public abstract Iterator<E> iterator();
  public abstract Object [] toArray();
  public abstract <T> T [] toArray(T [] a1);
  public abstract boolean add(E a1);
  public abstract boolean remove(Object a1);
  @Pure public abstract boolean containsAll(@GuardSatisfied Set<E> this,Collection<?> a1);
  public abstract boolean addAll(Collection<? extends E> a1);
  public abstract boolean retainAll(Collection<?> a1);
  public abstract boolean removeAll(Collection<?> a1);
  public abstract void clear();
  @Pure public abstract boolean equals(@GuardSatisfied Set<E> this,Object a1);
  @Pure public abstract int hashCode(@GuardSatisfied Set<E> this);
}
