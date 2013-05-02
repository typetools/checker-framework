package java.util;
import checkers.igj.quals.*;

@I
public interface ListIterator<E> extends @I Iterator<E> {
  public abstract boolean hasNext(@ReadOnly ListIterator<E> this);
  public abstract E next(@ReadOnly ListIterator<E> this);
  public abstract boolean hasPrevious(@ReadOnly ListIterator<E> this);
  public abstract E previous(@ReadOnly ListIterator<E> this);
  public abstract int nextIndex(@ReadOnly ListIterator<E> this);
  public abstract int previousIndex(@ReadOnly ListIterator<E> this);
  public abstract void remove(@Mutable ListIterator<E> this);
  public abstract void set(@Mutable ListIterator<E> this, E a1);
  public abstract void add(@Mutable ListIterator<E> this, E a1);
}
