package java.util;
import checkers.igj.quals.*;

@I
public interface Queue<E> extends @I Collection<E> {
  public abstract boolean add(@Mutable Queue this, E a1);
  public abstract boolean offer(@Mutable Queue this, E a1);
  public abstract E remove(@Mutable Queue this);
  public abstract E poll(@Mutable Queue this);
  public abstract E element(@ReadOnly Queue this);
  public abstract E peek(@ReadOnly Queue this);
}
