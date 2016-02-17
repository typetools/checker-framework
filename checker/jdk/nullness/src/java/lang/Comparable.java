package java.lang;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.PolyAll;

public interface Comparable<T extends @NonNull Object> {
  // argument may NOT be null
  @Pure int compareTo(@PolyAll @NonNull T a1);
}
