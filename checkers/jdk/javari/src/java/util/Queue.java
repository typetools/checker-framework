package java.util;
import checkers.javari.quals.*;

public interface Queue<E> extends Collection<E> {
  public abstract boolean add(E a1);
  public abstract boolean offer(E a1);
  public abstract E remove();
  public abstract E poll();
  public abstract E element(@ReadOnly Queue<E> this);
  public abstract E peek(@ReadOnly Queue<E> this);
}
