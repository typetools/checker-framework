package java.util;
import checkers.javari.quals.*;

public class Vector<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public Vector(int a1, int a2) { throw new RuntimeException(("skeleton method")); }
  public Vector(int a1) { throw new RuntimeException(("skeleton method")); }
  public Vector() { throw new RuntimeException(("skeleton method")); }
  public Vector(@PolyRead Vector this, @PolyRead Collection<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
  // copyInto is special-cased by the type-checker
  public synchronized void copyInto(@ReadOnly Vector this, @ReadOnly Object @Mutable [] a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized void trimToSize() { throw new RuntimeException(("skeleton method")); }
  public synchronized void ensureCapacity(int a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized void setSize(int a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized int capacity(@ReadOnly Vector this) { throw new RuntimeException(("skeleton method")); }
  public synchronized int size(@ReadOnly Vector this) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean isEmpty(@ReadOnly Vector this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Enumeration<E> elements(@PolyRead Vector this) { throw new RuntimeException(("skeleton method")); }
  public boolean contains(@ReadOnly Vector this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public int indexOf(@ReadOnly Vector this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized int indexOf(@ReadOnly Vector this, @ReadOnly Object a1, int a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized int lastIndexOf(@ReadOnly Vector this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized int lastIndexOf(@ReadOnly Vector this, @ReadOnly Object a1, int a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized E elementAt(@ReadOnly Vector this, int a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized E firstElement(@ReadOnly Vector this) { throw new RuntimeException(("skeleton method")); }
  public synchronized E lastElement(@ReadOnly Vector this) { throw new RuntimeException(("skeleton method")); }
  public synchronized void setElementAt(E a1, int a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized void removeElementAt(int a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized void insertElementAt(E a1, int a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized void addElement(E a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean removeElement(@ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized void removeAllElements() { throw new RuntimeException(("skeleton method")); }
  public synchronized Object[] toArray() { throw new RuntimeException(("skeleton method")); }
  public synchronized <T> T[] toArray(T[] a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized E get(@ReadOnly Vector this, int a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized E set(int a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean add(E a1) { throw new RuntimeException(("skeleton method")); }
  public boolean remove(@ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public void add(int a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized E remove(int a1) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean containsAll(@ReadOnly Vector this, @ReadOnly Collection<?> a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean addAll(@ReadOnly Collection<? extends E> a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean removeAll(@ReadOnly Collection<?> a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean retainAll(@ReadOnly Collection<?> a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean addAll(int a1, @ReadOnly Collection<? extends E> a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized boolean equals(@ReadOnly Vector this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public synchronized int hashCode(@ReadOnly Vector this) { throw new RuntimeException(("skeleton method")); }
  public synchronized String toString(@ReadOnly Vector this) { throw new RuntimeException(("skeleton method")); }
  public synchronized @PolyRead List<E> subList(@PolyRead Vector this, int a1, int a2) { throw new RuntimeException(("skeleton method")); }
  public synchronized Object clone() { throw new RuntimeException("skeleton method"); }
}
