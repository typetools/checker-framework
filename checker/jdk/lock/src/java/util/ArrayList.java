package java.util;

import org.checkerframework.checker.lock.qual.*;


// permits null elements
public class ArrayList<E extends Object> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 8683452581122892189L;
  public ArrayList(int a1) { throw new RuntimeException("skeleton method"); }
  public ArrayList() { throw new RuntimeException("skeleton method"); }
  public ArrayList(Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public void trimToSize(@GuardSatisfied ArrayList<E> this) { throw new RuntimeException("skeleton method"); }
  public void ensureCapacity(@GuardSatisfied ArrayList<E> this, int a1) { throw new RuntimeException("skeleton method"); }
   public int size(@GuardSatisfied ArrayList<E> this) { throw new RuntimeException("skeleton method"); }
   public boolean isEmpty(@GuardSatisfied ArrayList<E> this) { throw new RuntimeException("skeleton method"); }
  public boolean contains(@GuardSatisfied ArrayList<E> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public int indexOf(@GuardSatisfied ArrayList<E> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(@GuardSatisfied ArrayList<E> this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public Object [] toArray() { throw new RuntimeException("skeleton method"); }
  public <T> T [] toArray(T [] a1) { throw new RuntimeException("skeleton method"); }
  public E get(@GuardSatisfied ArrayList<E> this, int a1) { throw new RuntimeException("skeleton method"); }
  public E set(@GuardSatisfied ArrayList<E> this, int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public boolean add(@GuardSatisfied ArrayList<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  public void add(@GuardSatisfied ArrayList<E> this, int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public E remove(@GuardSatisfied ArrayList<E> this, int a1) { throw new RuntimeException("skeleton method"); }
  public boolean remove(@GuardSatisfied ArrayList<E> this, Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear(@GuardSatisfied ArrayList<E> this) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@GuardSatisfied ArrayList<E> this, Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  public boolean addAll(@GuardSatisfied ArrayList<E> this, int a1, Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
   public Object clone(@GuardSatisfied ArrayList<E> this) { throw new RuntimeException("skeleton method"); }
}
