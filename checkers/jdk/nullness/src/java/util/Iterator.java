package java.util;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")
@KeyForCovariant
public interface Iterator<E extends @Nullable Object> {
  public abstract boolean hasNext();
  public abstract E next();
  public abstract void remove();
}
