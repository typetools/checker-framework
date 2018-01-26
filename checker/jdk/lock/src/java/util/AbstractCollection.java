package java.util;
import org.checkerframework.checker.lock.qual.*;


import java.util.Collection;
import java.util.stream.Stream;

// Subclasses of this interface/class may opt to prohibit null elements
public abstract class AbstractCollection<E extends Object> implements Collection<E> {
  protected AbstractCollection() {}
  @Override
  public abstract Iterator<E> iterator();
  @Override
   public abstract int size(@GuardSatisfied AbstractCollection<E> this);
  @Override
   public boolean isEmpty(@GuardSatisfied AbstractCollection<E> this) { throw new RuntimeException("skeleton method"); }
  @Override
   public boolean contains(@GuardSatisfied AbstractCollection<E> this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  @Override
  public Object [] toArray() { throw new RuntimeException("skeleton method"); }
  @Override
  public <T> T [] toArray(T [] a1) { throw new RuntimeException("skeleton method"); }
  @Override
  public boolean add(@GuardSatisfied AbstractCollection<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  @Override
  public boolean remove(@GuardSatisfied AbstractCollection<E> this, Object a1) { throw new RuntimeException("skeleton method"); }
  @Override
  public boolean containsAll(@GuardSatisfied AbstractCollection<E> this, @GuardSatisfied Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  @Override
  public boolean addAll(@GuardSatisfied AbstractCollection<E> this, Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
  @Override
  public boolean removeAll(@GuardSatisfied AbstractCollection<E> this, Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  @Override
  public boolean retainAll(@GuardSatisfied AbstractCollection<E> this, Collection<?> a1) { throw new RuntimeException("skeleton method"); }
  @Override
  public void clear(@GuardSatisfied AbstractCollection<E> this) { throw new RuntimeException("skeleton method"); }
  @Override
   public String toString(@GuardSatisfied AbstractCollection<E> this) { throw new RuntimeException("skeleton method"); }
}
