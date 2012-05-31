package java.util;
import checkers.igj.quals.*;

@I
public interface Queue<E> extends @I Collection<E> {
  public abstract boolean add(E a1) @Mutable;
  public abstract boolean offer(E a1) @Mutable;
  public abstract E remove() @Mutable;
  public abstract E poll() @Mutable;
  public abstract E element() @ReadOnly;
  public abstract E peek() @ReadOnly;
}
