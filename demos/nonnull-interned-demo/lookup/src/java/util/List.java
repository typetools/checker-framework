package java.util;

import checkers.quals.*;

public abstract interface List<E> extends java.util.Collection<E> {
  public abstract int size();
  public abstract boolean isEmpty();
  public abstract boolean contains(java.lang.Object a1);
  public abstract @NonNull java.util.Iterator<E> iterator();
  public abstract java.lang.Object[] toArray();
  public abstract <T> @NonNull T[] toArray(T[] a1);
  public abstract boolean add(E a1);
  public abstract boolean remove(java.lang.Object a1);
  public abstract boolean containsAll(java.util.Collection<?> a1);
  public abstract boolean addAll(java.util.Collection<? extends E> a1);
  public abstract boolean addAll(int a1, java.util.Collection<? extends E> a2);
  public abstract boolean removeAll(java.util.Collection<?> a1);
  public abstract boolean retainAll(java.util.Collection<?> a1);
  public abstract void clear();
  public abstract boolean equals(java.lang.Object a1);
  public abstract int hashCode();
  public abstract E get(int a1);
  public abstract E set(int a1, E a2);
  public abstract void add(int a1, E a2);
  public abstract E remove(int a1);
  public abstract int indexOf(java.lang.Object a1);
  public abstract int lastIndexOf(java.lang.Object a1);
  public abstract @NonNull java.util.ListIterator<E> listIterator();
  public abstract @NonNull java.util.ListIterator<E> listIterator(int a1);
  public abstract @NonNull java.util.List<E> subList(int a1, int a2);
}
