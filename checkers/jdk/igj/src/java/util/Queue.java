package java.util;
import checkers.igj.quals.*;

@I
public interface Queue<E> extends @I Collection<E> {
  public abstract boolean add(@Mutable Queue<E> this, E a1);
  public abstract boolean offer(@Mutable Queue<E> this, E a1);
  public abstract E remove(@Mutable Queue<E> this);
  public abstract E poll(@Mutable Queue<E> this);
  public abstract E element(@ReadOnly Queue<E> this);
  public abstract E peek(@ReadOnly Queue<E> this);
}
