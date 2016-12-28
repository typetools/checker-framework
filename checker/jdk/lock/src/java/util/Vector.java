package java.util;

import org.checkerframework.checker.lock.qual.*;


// permits nullable object
public class Vector<E extends Object> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
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
   public synchronized int size(@GuardSatisfied Vector<E> this) { throw new RuntimeException("skeleton method"); }
   public synchronized boolean isEmpty(@GuardSatisfied Vector<E> this) { throw new RuntimeException("skeleton method"); }
  public Enumeration<E> elements() { throw new RuntimeException("skeleton method"); }
   public boolean contains(@GuardSatisfied Vector<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int indexOf(@GuardSatisfied Vector<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public synchronized int indexOf(@GuardSatisfied Vector<E> this,@GuardSatisfied Object a1, int a2) { throw new RuntimeException("skeleton method"); }
   public synchronized int lastIndexOf(@GuardSatisfied Vector<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public synchronized int lastIndexOf(@GuardSatisfied Vector<E> this,@GuardSatisfied Object a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized E elementAt(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized E firstElement() { throw new RuntimeException("skeleton method"); }
  public synchronized E lastElement() { throw new RuntimeException("skeleton method"); }
  public synchronized void setElementAt(E a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void removeElementAt(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void insertElementAt(E a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void addElement(E a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean removeElement(Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void removeAllElements() { throw new RuntimeException("skeleton method"); }
  public synchronized Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public synchronized <T> T [] toArray(T [] a1) { throw new RuntimeException("skeleton method"); }
   public synchronized E get(@GuardSatisfied Vector<E> this,int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized E set(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean add(E a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(Object a1) { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public synchronized E remove(int a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
   public synchronized boolean containsAll(@GuardSatisfied Vector<E> this,@GuardSatisfied Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean addAll(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean removeAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean retainAll(Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean addAll(int a1, Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
   public synchronized boolean equals(@GuardSatisfied Vector<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public synchronized int hashCode(@GuardSatisfied Vector<E> this) { throw new RuntimeException("skeleton method"); }
   public synchronized String toString(@GuardSatisfied Vector<E> this) { throw new RuntimeException("skeleton method"); }
   public synchronized List<E> subList(@GuardSatisfied Vector<E> this,int a1, int a2) { throw new RuntimeException("skeleton method"); }
   public synchronized Object clone(@GuardSatisfied Vector<E> this) { throw new RuntimeException("skeleton method"); }
}
