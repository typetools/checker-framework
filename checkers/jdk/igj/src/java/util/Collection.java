package java.util;
import checkers.igj.quals.*;

@I
public interface Collection<E> extends @I java.lang.Iterable<E> {
  public abstract int size() @ReadOnly;
  public abstract boolean isEmpty() @ReadOnly;
  public abstract boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly;
  public abstract @I java.util.Iterator<E> iterator() @ReadOnly;
  public abstract java.lang.Object[] toArray() @ReadOnly;
  public abstract <T> T[] toArray(T[] a1) @ReadOnly ;
  public abstract boolean add(E a1) @Mutable;
  public abstract boolean remove(@ReadOnly java.lang.Object a1) @Mutable;
  public abstract boolean containsAll(@ReadOnly java.util.Collection<?> a1) @ReadOnly;
  public abstract boolean addAll(@ReadOnly java.util.Collection<? extends E> a1) @Mutable;
  public abstract boolean removeAll(@ReadOnly java.util.Collection<?> a1) @Mutable;
  public abstract boolean retainAll(@ReadOnly java.util.Collection<?> a1) @Mutable;
  public abstract void clear() @Mutable;
  public abstract boolean equals(@ReadOnly java.lang.Object a1) @ReadOnly;
  public abstract int hashCode() @ReadOnly;
}
