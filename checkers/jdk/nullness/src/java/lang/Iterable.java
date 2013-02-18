package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier(checkers.nullness.quals.NonNull.class)

public abstract interface Iterable<T extends @Nullable Object> {
  public abstract java.util.Iterator<T> iterator();
}
