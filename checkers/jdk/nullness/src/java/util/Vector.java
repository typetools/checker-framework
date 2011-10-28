package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// permits nullable object
public class Vector<E extends @Nullable Object> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public Vector(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public Vector(int a1) { throw new RuntimeException("skeleton method"); }
  public Vector() { throw new RuntimeException("skeleton method"); }
  public Vector(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void copyInto(Object[] a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void trimToSize() { throw new RuntimeException("skeleton method"); }
  public synchronized void ensureCapacity(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void setSize(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int capacity() { throw new RuntimeException("skeleton method"); }
  public synchronized int size() { throw new RuntimeException("skeleton method"); }
  public synchronized boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public Enumeration<E> elements() { throw new RuntimeException("skeleton method"); }
  public boolean contains(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int indexOf(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int indexOf(@Nullable Object a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized int lastIndexOf(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int lastIndexOf(@Nullable Object a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized E elementAt(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized E firstElement() { throw new RuntimeException("skeleton method"); }
  public synchronized E lastElement() { throw new RuntimeException("skeleton method"); }
  public synchronized void setElementAt(E a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void removeElementAt(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void insertElementAt(E a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void addElement(E a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean removeElement(Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void removeAllElements() { throw new RuntimeException("skeleton method"); }
  public synchronized @Nullable Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public synchronized <T> @Nullable T @PolyNull [] toArray(T @PolyNull [] a1) { throw new RuntimeException("skeleton method"); }
  public synchronized @Pure E get(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized E set(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public synchronized E remove(int a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public synchronized boolean containsAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean addAll(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean removeAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean retainAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean addAll(int a1, Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int hashCode() { throw new RuntimeException("skeleton method"); }
  public synchronized String toString() { throw new RuntimeException("skeleton method"); }
  public synchronized List<E> subList(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized Object clone() { throw new RuntimeException("skeleton method"); }
}
