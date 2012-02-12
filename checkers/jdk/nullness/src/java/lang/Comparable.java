package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface Comparable<T extends @NonNull Object> {
  // argument may not be null
  public abstract int compareTo(T a1);
}
