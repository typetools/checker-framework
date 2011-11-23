package java.util;
import checkers.igj.quals.*;

@I
public interface ListIterator<E> extends @I Iterator<E> {
  public abstract boolean hasNext() @ReadOnly;
  public abstract E next() @ReadOnly;
  public abstract boolean hasPrevious() @ReadOnly;
  public abstract E previous() @ReadOnly;
  public abstract int nextIndex() @ReadOnly;
  public abstract int previousIndex() @ReadOnly;
  public abstract void remove() @Mutable;
  public abstract void set(E a1) @Mutable;
  public abstract void add(E a1) @Mutable;
}
