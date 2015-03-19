package java.util;
import org.checkerframework.checker.igj.qual.*;

@I
public interface Iterator<E extends @ReadOnly Object> {
  public abstract boolean hasNext(@ReadOnly Iterator<E> this);
  // For a justification of this annotation, see section
  // "Iterators and their abstract state" in the Checker Framework Manual.
  public abstract E next(@ReadOnly Iterator<E> this);
  public abstract void remove(@Mutable Iterator<E> this);
}
