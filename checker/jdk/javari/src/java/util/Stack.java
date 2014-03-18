package java.util;
import org.checkerframework.checker.javari.qual.*;

public class Stack<E> extends Vector<E> {
    private static final long serialVersionUID = 0L;
    public Stack() { throw new RuntimeException("skeleton method"); }
    public E push(E item) { throw new RuntimeException("skeleton method"); }
    public synchronized E pop() { throw new RuntimeException("skeleton method"); }
    public synchronized E peek(@ReadOnly Stack<E> this) { throw new RuntimeException("skeleton method"); }
    public boolean empty(@ReadOnly Stack<E> this) { throw new RuntimeException("skeleton method"); }
    public synchronized int search(@ReadOnly Stack<E> this, @ReadOnly Object o) { throw new RuntimeException("skeleton method"); }
}
