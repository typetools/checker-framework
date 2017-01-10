package java.util;

import org.checkerframework.checker.lock.qual.*;

// Subclasses of this interface/class may opt to prohibit null elements
public interface Queue<E extends Object> extends Collection<E> {
  public abstract boolean add(E a1);
  public abstract boolean offer(E a1);
  public abstract E remove();
  public abstract E poll();
  public abstract E element();
  public abstract E peek();

   public abstract boolean isEmpty(@GuardSatisfied Queue<E> this);
}
