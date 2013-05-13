package java.util;
import checkers.nullness.quals.Nullable;
import dataflow.quals.Pure;

public interface Comparator<T> {
  public abstract int compare(T a1, T a2);
  @Pure public abstract boolean equals(@Nullable Object a1);
}
