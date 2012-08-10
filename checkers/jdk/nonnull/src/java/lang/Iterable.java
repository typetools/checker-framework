package java.lang;

import checkers.nonnull.quals.Nullable;


public abstract interface Iterable<T extends @Nullable Object> {
  public abstract java.util.Iterator<T> iterator();
}
