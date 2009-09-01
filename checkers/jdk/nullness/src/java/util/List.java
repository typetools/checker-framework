package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// Subclasses of this interface/class may opt to prohibit null elements
public interface List<E extends @NonNull Object> extends java.util.Collection<E> {
  public abstract int size();
  public abstract boolean isEmpty();
  public abstract boolean contains(@Nullable java.lang.Object a1);
  public abstract java.util.Iterator<E> iterator();
  // Element annotation should be the same as that on the type parameter E.
  // It's @Nullable here because that is most lenient.
  // Eventually, figure out how to express this, or hard-code in the checker.
  public abstract java.lang.Object [] toArray();
  // @Nullable because, if there is room in the argument a1, the method
  // puts null after the elements of this.
  public abstract <T> @Nullable T [] toArray(T[] a1);
  public abstract boolean add(E a1);
  public abstract boolean remove(@Nullable java.lang.Object a1);
  public abstract boolean containsAll(java.util.Collection<?> a1);
  public abstract boolean addAll(java.util.Collection<? extends E> a1);
  public abstract boolean addAll(int a1, java.util.Collection<? extends E> a2);
  public abstract boolean removeAll(java.util.Collection<?> a1);
  public abstract boolean retainAll(java.util.Collection<?> a1);
  public abstract void clear();
  public abstract boolean equals(@Nullable java.lang.Object a1);
  public abstract int hashCode();
  public abstract E get(int a1);
  public abstract E set(int a1, E a2);
  public abstract void add(int a1, E a2);
  public abstract E remove(int a1);
  public abstract int indexOf(@Nullable java.lang.Object a1);
  public abstract int lastIndexOf(@Nullable java.lang.Object a1);
  public abstract java.util.ListIterator<E> listIterator();
  public abstract java.util.ListIterator<E> listIterator(int a1);
  public abstract java.util.List<E> subList(int a1, int a2);
}
