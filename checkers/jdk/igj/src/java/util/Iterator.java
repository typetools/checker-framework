package java.util;
import checkers.igj.quals.*;

@I
public interface Iterator<E> {
  public abstract boolean hasNext() @ReadOnly;
  // For a justification of this annotation, see section
  // "Iterators and their abstract state" in the Checker Framework manual.
  public abstract E next() @ReadOnly;
  public abstract void remove() @Mutable;
}
