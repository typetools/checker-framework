package java.util;
import checkers.igj.quals.*;

@I
public interface List<E> extends @I java.util.Collection<E> {
  public abstract int size() @ReadOnly ;
  public abstract boolean isEmpty() @ReadOnly ;
  public abstract boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly;
  public abstract @I java.util.Iterator<E> iterator() @ReadOnly;
  public abstract java.lang.Object[] toArray() @ReadOnly;
  public abstract <T> T[] toArray(@Mutable T[] a1) @ReadOnly;
  public abstract boolean add(E a1) @Mutable;
  public abstract boolean remove(@ReadOnly java.lang.Object a1) @Mutable;
  public abstract boolean containsAll(@ReadOnly java.util.Collection<?> a1) @ReadOnly;
  public abstract boolean addAll(@ReadOnly java.util.Collection<? extends E> a1) @Mutable;
  public abstract boolean addAll(int a1, @ReadOnly java.util.Collection<? extends E> a2) @Mutable;
  public abstract boolean removeAll(@ReadOnly java.util.Collection<?> a1) @Mutable;
  public abstract boolean retainAll(@ReadOnly java.util.Collection<?> a1) @Mutable;
  public abstract void clear() @Mutable;
  public abstract boolean equals(@ReadOnly java.lang.Object a1) @ReadOnly ;
  public abstract int hashCode() @ReadOnly;
  public abstract E get(int a1) @ReadOnly;
  public abstract E set(int a1, E a2) @Mutable;
  public abstract void add(int a1, E a2) @Mutable;
  public abstract E remove(int a1) @Mutable;
  public abstract int indexOf(@ReadOnly java.lang.Object a1) @ReadOnly;
  public abstract int lastIndexOf(@ReadOnly java.lang.Object a1) @ReadOnly;
  public abstract @I java.util.ListIterator<E> listIterator() @ReadOnly;
  public abstract @I java.util.ListIterator<E> listIterator(int a1) @ReadOnly;
  public abstract @I java.util.List<E> subList(int a1, int a2) @ReadOnly ;
}
