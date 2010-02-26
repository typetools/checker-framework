package java.util;
import checkers.igj.quals.*;

@I
public class Vector<E> extends @I AbstractList<E> implements @I List<E>, @I RandomAccess, @I Cloneable, @I java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public Vector(int a1, int a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Vector(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Vector() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Vector(@ReadOnly Collection<? extends E> a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public synchronized void copyInto(Object @Mutable [] a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized void trimToSize() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized void ensureCapacity(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void setSize(int a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized int capacity() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public Enumeration<E> elements() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean contains(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int indexOf(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized int indexOf(@ReadOnly Object a1, int a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized int lastIndexOf(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized int lastIndexOf(@ReadOnly Object a1, int a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized E elementAt(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized E firstElement() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized E lastElement() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized void setElementAt(E a1, int a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized void removeElementAt(int a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized void insertElementAt(E a1, int a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized void addElement(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized boolean removeElement(@ReadOnly Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized void removeAllElements() @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized Object[] toArray() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized <T> T[] toArray(T[] a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized E get(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized E set(int a1, E a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized boolean add(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean remove(@ReadOnly Object a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized E remove(int a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void clear() @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized boolean containsAll(@ReadOnly Collection<?> a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized boolean addAll(@ReadOnly Collection<? extends E> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized boolean removeAll(@ReadOnly Collection<?> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized boolean retainAll(@ReadOnly Collection<?> a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized boolean addAll(int a1, @ReadOnly Collection<? extends E> a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized boolean equals(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized @I List<E> subList(int a1, int a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
