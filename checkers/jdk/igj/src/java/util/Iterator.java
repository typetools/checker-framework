package java.util;
import checkers.igj.quals.*;

@I
public interface Iterator<E> {
  public abstract boolean hasNext(@ReadOnly Iterator this);
  // For a justification of this annotation, see section
  // "Iterators and their abstract state" in the Checker Framework manual.
  public abstract E next(@ReadOnly Iterator this);
  public abstract void remove(@Mutable Iterator this);
}
