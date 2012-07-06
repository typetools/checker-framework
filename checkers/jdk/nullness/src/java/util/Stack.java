package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// permits null elements
public class Stack<E extends @Nullable Object> extends Vector<E> {
  private static final long serialVersionUID = 0;
  public Stack() { throw new RuntimeException("skeleton method"); }
  public E push(E a1) { throw new RuntimeException("skeleton method"); }
  public synchronized E pop() { throw new RuntimeException("skeleton method"); }
  public synchronized E peek() { throw new RuntimeException("skeleton method"); }
  public boolean empty() { throw new RuntimeException("skeleton method"); }
  public synchronized int search(Object a1) { throw new RuntimeException("skeleton method"); }
}
