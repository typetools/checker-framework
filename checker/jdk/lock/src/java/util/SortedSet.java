package java.util;
import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit null elements
public interface SortedSet<E extends Object> extends Set<E> {
   public abstract Comparator<? super E> comparator(@GuardSatisfied SortedSet<E> this);
   public abstract SortedSet<E> subSet(@GuardSatisfied SortedSet<E> this,@GuardSatisfied E a1, @GuardSatisfied E a2);
   public abstract SortedSet<E> headSet(@GuardSatisfied SortedSet<E> this,E a1);
   public abstract SortedSet<E> tailSet(@GuardSatisfied SortedSet<E> this,E a1);
   public abstract E first(@GuardSatisfied SortedSet<E> this);
   public abstract E last(@GuardSatisfied SortedSet<E> this);
}
