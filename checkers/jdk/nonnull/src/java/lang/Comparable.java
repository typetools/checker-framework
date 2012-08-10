package java.lang;

import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;


public abstract interface Comparable<T extends @NonNull Object> {
  // argument may not be null
  public abstract int compareTo(T a1);
}
