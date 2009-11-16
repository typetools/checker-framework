package java.util;

import checkers.igj.quals.*;

@I
public interface Comparator<T> {
  public abstract int compare(T a1, T a2) @ReadOnly;
  public abstract boolean equals(@ReadOnly java.lang.Object a1) @ReadOnly;
}
