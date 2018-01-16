package java.util;


// permits null elements
import org.checkerframework.checker.lock.qual.GuardSatisfied;

public class Stack<E> extends Vector<E> {
  private static final long serialVersionUID = 0;
  public Stack() { throw new RuntimeException("skeleton method"); }
  public E push(@GuardSatisfied Stack<E> this, E a1) { throw new RuntimeException("skeleton method"); }
  public synchronized E pop(@GuardSatisfied Stack<E> this) { throw new RuntimeException("skeleton method"); }
  public synchronized E peek() { throw new RuntimeException("skeleton method"); }
  public boolean empty() { throw new RuntimeException("skeleton method"); }
  public synchronized int search(Object a1) { throw new RuntimeException("skeleton method"); }
}
