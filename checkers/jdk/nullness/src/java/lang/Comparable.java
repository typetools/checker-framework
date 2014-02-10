package java.lang;

import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import dataflow.quals.Pure;


public abstract interface Comparable<T extends @NonNull Object> {
  // argument may NOT be null
  @Pure public abstract int compareTo(T a1);
}
