package java.lang;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.dataflow.qual.Pure;

public interface Comparable<T extends @NonNull Object> {
  // argument may NOT be null
  @Pure int compareTo(T a1);
}
