package java.util;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class AbstractList<E extends Object> extends AbstractCollection<E> implements List<E> {
  protected AbstractList() {}
  public boolean add(E a1) { throw new RuntimeException("skeleton method"); }
   public abstract E get(@GuardSatisfied AbstractList<E> this,int a1);
  public E set(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public void add(int a1, E a2) { throw new RuntimeException("skeleton method"); }
  public E remove(int a1) { throw new RuntimeException("skeleton method"); }
   public int indexOf(@GuardSatisfied AbstractList<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int lastIndexOf(@GuardSatisfied AbstractList<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  public boolean addAll(int a1, Collection<? extends E> a2) { throw new RuntimeException("skeleton method"); }
  public Iterator<E> iterator() { throw new RuntimeException("skeleton method"); }
  public ListIterator<E> listIterator() { throw new RuntimeException("skeleton method"); }
  public ListIterator<E> listIterator(int a1) { throw new RuntimeException("skeleton method"); }
   public List<E> subList(@GuardSatisfied AbstractList<E> this,int a1, int a2) { throw new RuntimeException("skeleton method"); }
   public boolean equals(@GuardSatisfied AbstractList<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied AbstractList<E> this) { throw new RuntimeException("skeleton method"); }
}
