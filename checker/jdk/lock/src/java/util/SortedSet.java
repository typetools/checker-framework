package java.util;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit null elements
public interface SortedSet<E extends Object> extends Set<E> {
  @SideEffectFree public abstract Comparator<? super E> comparator(@GuardSatisfied SortedSet<E> this);
  @SideEffectFree public abstract SortedSet<E> subSet(@GuardSatisfied SortedSet<E> this,@GuardSatisfied E a1, @GuardSatisfied E a2);
  @SideEffectFree public abstract SortedSet<E> headSet(@GuardSatisfied SortedSet<E> this,E a1);
  @SideEffectFree public abstract SortedSet<E> tailSet(@GuardSatisfied SortedSet<E> this,E a1);
  @SideEffectFree public abstract E first(@GuardSatisfied SortedSet<E> this);
  @SideEffectFree public abstract E last(@GuardSatisfied SortedSet<E> this);
}
