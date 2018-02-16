package java.util;
import org.checkerframework.checker.lock.qual.*;


import java.util.stream.Stream;

// Subclasses of this interface/class may opt to prohibit null elements
public interface Collection<E extends Object> extends Iterable<E> {
   int size(@GuardSatisfied Collection<E> this);
   boolean isEmpty(@GuardSatisfied Collection<E> this);
  // not true, because map could contain nulls:  AssertParametersNonNull("get(#1)")
   boolean contains(@GuardSatisfied Collection<E> this, @GuardSatisfied Object a1);
  @Override
  Iterator<E> iterator();
  // The Nullness Checker does NOT use these signatures for either version
  // of toArray; rather, the checker has hard-coded rules for those two
  // methods, because the most useful type for toArray is not expressible
  // in the surface syntax that the nullness annotations support.
  Object [] toArray();
  <T extends Object> T [] toArray(T [] a1);
  boolean add(@GuardSatisfied Collection<E> this, E a1);
  boolean remove(@GuardSatisfied Collection<E> this, Object a1);
   public abstract boolean containsAll(@GuardSatisfied Collection<E> this, @GuardSatisfied Collection<?> a1);
  boolean addAll(@GuardSatisfied Collection<E> this, Collection<? extends E> a1);
  boolean removeAll(@GuardSatisfied Collection<E> this, Collection<?> a1);
  boolean retainAll(@GuardSatisfied Collection<E> this, Collection<?> a1);
  void clear(@GuardSatisfied Collection<E> this);
  @Override
   public abstract boolean equals(@GuardSatisfied Collection<E> this, @GuardSatisfied Object a1);
  @Override
   public abstract int hashCode(@GuardSatisfied Collection<E> this);
  default Stream<E> stream() { throw new RuntimeException("skeleton method"); }
}
