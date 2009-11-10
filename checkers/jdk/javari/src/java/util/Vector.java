package java.util;
import checkers.javari.quals.*;

public class Vector<E> extends java.util.AbstractList<E> implements java.util.List<E>, java.util.RandomAccess, java.lang.Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public Vector(int a1, int a2) { throw new RuntimeException(("skeleton method")); }
  public Vector(int a1) { throw new RuntimeException(("skeleton method")); }
  public Vector() { throw new RuntimeException(("skeleton method")); }
  public Vector(@PolyRead java.util.Collection<? extends E> a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public synchronized void copyInto(@PolyRead java.lang.Object [] a1) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public synchronized void trimToSize() { throw new RuntimeException(("skeleton method")); }
  public synchronized void ensureCapacity(int a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized void setSize(int a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized int capacity() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized int size() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean isEmpty() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public @PolyRead java.util.Enumeration<E> elements() @PolyRead { throw new RuntimeException(("skeleton method")); }
  public boolean contains(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public int indexOf(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized int indexOf(@ReadOnly java.lang.Object a1, int a2) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized int lastIndexOf(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized int lastIndexOf(@ReadOnly java.lang.Object a1, int a2) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized E elementAt(int a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized E firstElement() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized E lastElement() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized void setElementAt(E a1, int a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized void removeElementAt(int a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized void insertElementAt(E a1, int a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized void addElement(E a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean removeElement(@ReadOnly java.lang.Object a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized void removeAllElements() { throw new RuntimeException(("skeleton method")); }
  public synchronized java.lang.Object[] toArray() { throw new RuntimeException(("skeleton method")); }
  public synchronized <T> T[] toArray(T[] a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized E get(int a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized E set(int a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean add(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean remove(@ReadOnly java.lang.Object a1) { throw new RuntimeException(("skeleton method")); }
  public void add(int a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized E remove(int a1) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean containsAll(@ReadOnly java.util.Collection<?> a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean addAll(@ReadOnly java.util.Collection<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean removeAll(@ReadOnly java.util.Collection<?> a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean retainAll(@ReadOnly java.util.Collection<?> a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean addAll(int a1, @ReadOnly java.util.Collection<? extends E> a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean equals(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized int hashCode() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized java.lang.String toString() @ReadOnly { throw new RuntimeException(("skeleton method")); }
  public synchronized @PolyRead java.util.List<E> subList(int a1, int a2) @PolyRead { throw new RuntimeException(("skeleton method")); }
  public synchronized Object clone() { throw new RuntimeException("skeleton method"); }
}
