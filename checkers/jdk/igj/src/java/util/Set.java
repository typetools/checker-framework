package java.util;
import checkers.igj.quals.*;

@I
public interface Set<E> extends @I Collection<E> {
  public abstract int size() @ReadOnly;
  public abstract boolean isEmpty() @ReadOnly;
  public abstract boolean contains(Object a1) @ReadOnly;
  public abstract @I Iterator<E> iterator() @ReadOnly;
  public abstract Object[] toArray() @ReadOnly;
  public abstract <T> T[] toArray(T[] a1) @ReadOnly;
  public abstract boolean add(E a1) @Mutable;
  public abstract boolean remove(@ReadOnly Object a1) @Mutable;
  public abstract boolean containsAll(@ReadOnly Collection<?> a1) @ReadOnly;
  public abstract boolean addAll(@ReadOnly Collection<? extends E> a1) @Mutable;
  public abstract boolean retainAll(@ReadOnly Collection<?> a1) @Mutable;
  public abstract boolean removeAll(@ReadOnly Collection<?> a1) @Mutable;
  public abstract void clear() @Mutable;
  public abstract boolean equals(@ReadOnly Object a1) @ReadOnly;
  public abstract int hashCode() @ReadOnly;
}
