package java.lang;

import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;


public abstract interface Comparable<T extends @NonNull Object> {
  // argument may not be null
  public abstract int compareTo(T a1);
}
