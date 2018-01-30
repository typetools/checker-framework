package java.util;
import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit null elements
public interface Set<E extends Object> extends Collection<E> {
  public abstract int size(@GuardSatisfied Set<E> this);
  public abstract boolean isEmpty(@GuardSatisfied Set<E> this);
  public abstract boolean contains(@GuardSatisfied Set<E> this, @GuardSatisfied Object a1);
  public abstract Iterator<E> iterator();
  public abstract Object [] toArray();
  public abstract <T> T [] toArray(T [] a1);
  public abstract boolean add(@GuardSatisfied Set<E> this, E a1);
  public abstract boolean remove(@GuardSatisfied Set<E> this, Object a1);
  public abstract boolean containsAll(@GuardSatisfied Set<E> this, @GuardSatisfied Collection<?> a1);
  public abstract boolean addAll(@GuardSatisfied Set<E> this, Collection<? extends E> a1);
  public abstract boolean retainAll(@GuardSatisfied Set<E> this, Collection<?> a1);
  public abstract boolean removeAll(@GuardSatisfied Set<E> this, Collection<?> a1);
  public abstract void clear(@GuardSatisfied Set<E> this);
  public abstract boolean equals(@GuardSatisfied Set<E> this, @GuardSatisfied Object a1);
  public abstract int hashCode(@GuardSatisfied Set<E> this);
}
