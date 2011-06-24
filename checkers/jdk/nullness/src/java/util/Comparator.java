package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public interface Comparator<T> {
  public abstract int compare(T a1, T a2);
  public abstract boolean equals(@Nullable Object a1);
}
