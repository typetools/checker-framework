package java.lang;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;


public abstract interface Comparable<T extends @NonNull Object> {
  // argument may NOT be null
  @Pure public abstract int compareTo(T a1);
}
