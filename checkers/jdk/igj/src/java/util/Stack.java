package java.util;
import checkers.igj.quals.*;

@I
public class Stack<E> extends @I Vector<E> {
    private static final long serialVersionUID = 0L;
  public Stack() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public E push(E a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized E pop() @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized E peek() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean empty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized int search(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
}
