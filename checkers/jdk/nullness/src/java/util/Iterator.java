package java.util;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")
// This @Covariant annotation is sound, but it would not be sound on
// ListIterator (a subclass of Iterator), which supports a set operation.
@Covariant(0)
public interface Iterator<E extends @Nullable Object> {
  public abstract boolean hasNext();
  public abstract E next();
  public abstract void remove();
}
