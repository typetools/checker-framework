package java.util;
import checkers.igj.quals.*;

@I
public interface Iterator<E> {
  public abstract boolean hasNext(@ReadOnly Iterator<E> this);
  // For a justification of this annotation, see section
  // "Iterators and their abstract state" in the Checker Framework manual.
  public abstract E next(@ReadOnly Iterator<E> this);
  public abstract void remove(@Mutable Iterator<E> this);
}
