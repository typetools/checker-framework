package java.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.Covariant;

// This @Covariant annotation is sound, but it would not be sound on
// ListIterator (a subclass of Iterator), which supports a set operation.
@Covariant(0)
public interface Iterator<E extends @Nullable Object> {
  public abstract boolean hasNext();
  public abstract E next();
  public abstract void remove();
}
