package java.util;
import checkers.igj.quals.*;

@I
public interface ListIterator<E> extends @I Iterator<E> {
  public abstract boolean hasNext(@ReadOnly ListIterator this);
  public abstract E next(@ReadOnly ListIterator this);
  public abstract boolean hasPrevious(@ReadOnly ListIterator this);
  public abstract E previous(@ReadOnly ListIterator this);
  public abstract int nextIndex(@ReadOnly ListIterator this);
  public abstract int previousIndex(@ReadOnly ListIterator this);
  public abstract void remove(@Mutable ListIterator this);
  public abstract void set(@Mutable ListIterator this, E a1);
  public abstract void add(@Mutable ListIterator this, E a1);
}
