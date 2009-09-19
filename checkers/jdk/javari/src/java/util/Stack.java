package java.util;
import checkers.javari.quals.*;

public class Stack<E> extends Vector<E> {
    public Stack() { throw new RuntimeException("skeleton method"); }
    public E push(E item) { throw new RuntimeException("skeleton method"); }
    public synchronized E pop() { throw new RuntimeException("skeleton method"); }
    public synchronized E peek() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public boolean empty() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public synchronized int search(@ReadOnly Object o) @ReadOnly { throw new RuntimeException("skeleton method"); }
}
