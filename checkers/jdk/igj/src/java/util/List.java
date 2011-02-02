package java.util;
import checkers.igj.quals.*;

@I
public interface List<E> extends @I Collection<E> {
  public abstract int size() @ReadOnly ;
  public abstract boolean isEmpty() @ReadOnly ;
  public abstract boolean contains(@ReadOnly Object a1) @ReadOnly;
  public abstract @I Iterator<E> iterator() @ReadOnly;
  public abstract Object[] toArray() @ReadOnly;
  public abstract <T> T[] toArray(T @Mutable [] a1) @ReadOnly;
  public abstract boolean add(E a1) @Mutable;
  public abstract boolean remove(@ReadOnly Object a1) @Mutable;
  public abstract boolean containsAll(@ReadOnly Collection<?> a1) @ReadOnly;
  public abstract boolean addAll(@ReadOnly Collection<? extends E> a1) @Mutable;
  public abstract boolean addAll(int a1, @ReadOnly Collection<? extends E> a2) @Mutable;
  public abstract boolean removeAll(@ReadOnly Collection<?> a1) @Mutable;
  public abstract boolean retainAll(@ReadOnly Collection<?> a1) @Mutable;
  public abstract void clear() @Mutable;
  public abstract boolean equals(@ReadOnly Object a1) @ReadOnly ;
  public abstract int hashCode() @ReadOnly;
  public abstract E get(int a1) @ReadOnly;
  public abstract E set(int a1, E a2) @Mutable;
  public abstract void add(int a1, E a2) @Mutable;
  public abstract E remove(int a1) @Mutable;
  public abstract int indexOf(@ReadOnly Object a1) @ReadOnly;
  public abstract int lastIndexOf(@ReadOnly Object a1) @ReadOnly;
  public abstract @I ListIterator<E> listIterator() @ReadOnly;
  public abstract @I ListIterator<E> listIterator(int a1) @ReadOnly;
  public abstract @I List<E> subList(int a1, int a2) @ReadOnly ;
}
