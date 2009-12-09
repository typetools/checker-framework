package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// permits nullable object
public class Vector<E extends @Nullable Object> extends java.util.AbstractList<E> implements java.util.List<E>, java.util.RandomAccess, java.lang.Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public Vector(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public Vector(int a1) { throw new RuntimeException("skeleton method"); }
  public Vector() { throw new RuntimeException("skeleton method"); }
  public Vector(java.util.Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void copyInto(java.lang.Object[] a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void trimToSize() { throw new RuntimeException("skeleton method"); }
  public synchronized void ensureCapacity(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void setSize(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int capacity() { throw new RuntimeException("skeleton method"); }
  public synchronized int size() { throw new RuntimeException("skeleton method"); }
  public synchronized boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public java.util.Enumeration<E> elements() { throw new RuntimeException("skeleton method"); }
  public boolean contains(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public int indexOf(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int indexOf(@Nullable java.lang.Object a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized int lastIndexOf(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int lastIndexOf(@Nullable java.lang.Object a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized E elementAt(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized E firstElement() { throw new RuntimeException("skeleton method"); }
  public synchronized E lastElement() { throw new RuntimeException("skeleton method"); }
  public synchronized void setElementAt(E a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void removeElementAt(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void insertElementAt(E a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void addElement(E a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean removeElement(java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void removeAllElements() { throw new RuntimeException("skeleton method"); }
  public synchronized @Nullable java.lang.Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public synchronized <T> @Nullable T [] toArray(T[] a1) { throw new RuntimeException("skeleton method"); }
  public synchronized E get(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized E set(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public synchronized E remove(int a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public synchronized boolean containsAll(java.util.Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean addAll(java.util.Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean removeAll(java.util.Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean retainAll(java.util.Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean addAll(int a1, java.util.Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int hashCode() { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public synchronized java.util.List<E> subList(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized Object clone() { throw new RuntimeException("skeleton method"); }
}
