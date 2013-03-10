package java.util;
import checkers.nonnull.quals.Nullable;

public interface Comparator<T> {
  public abstract int compare(T a1, T a2);
  public abstract boolean equals(@Nullable Object a1);
}
