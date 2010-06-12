package java.util;
import checkers.igj.quals.*;

@I
public interface Iterator<E> {
  public abstract boolean hasNext() @ReadOnly;
  public abstract E next() @ReadOnly;
  public abstract void remove() @Mutable;
}
