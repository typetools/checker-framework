package java.util;
import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit null elements
public interface NavigableSet<E extends Object> extends SortedSet<E> {
  public abstract E lower(E a1);
  public abstract E floor(E a1);
  public abstract E ceiling(E a1);
  public abstract E higher(E a1);
  public abstract E pollFirst(@GuardSatisfied NavigableSet<E> this);
  public abstract E pollLast(@GuardSatisfied NavigableSet<E> this);
  public abstract Iterator<E> iterator();
  public abstract NavigableSet<E> descendingSet();
  public abstract Iterator<E> descendingIterator();
  public abstract NavigableSet<E> subSet(@GuardSatisfied NavigableSet<E> this, @GuardSatisfied E a1, boolean a2, @GuardSatisfied E a3, boolean a4);
  public abstract NavigableSet<E> headSet(@GuardSatisfied NavigableSet<E> this, @GuardSatisfied E a1, boolean a2);
  public abstract NavigableSet<E> tailSet(@GuardSatisfied NavigableSet<E> this, @GuardSatisfied E a1, boolean a2);
  public abstract SortedSet<E> subSet(@GuardSatisfied NavigableSet<E> this, @GuardSatisfied E a1, @GuardSatisfied E a2);
  public abstract SortedSet<E> headSet(@GuardSatisfied NavigableSet<E> this, E a1);
  public abstract SortedSet<E> tailSet(@GuardSatisfied NavigableSet<E> this, E a1);


   public abstract boolean isEmpty(@GuardSatisfied NavigableSet<E> this);

}
