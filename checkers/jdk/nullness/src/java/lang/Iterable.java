package java.lang;

import checkers.nullness.quals.Nullable;


public abstract interface Iterable<T extends @Nullable Object> {
  public abstract java.util.Iterator<T> iterator();
}
