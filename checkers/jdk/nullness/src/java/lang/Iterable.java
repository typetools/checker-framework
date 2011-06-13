package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface Iterable<T extends @Nullable Object> {
  public abstract java.util.Iterator<T> iterator();
}
