package java.util;

import checkers.igj.quals.*;

@I
public interface Comparator<T> {
  public abstract int compare(@ReadOnly Comparator<T> this, T a1, T a2);
  public abstract boolean equals(@ReadOnly Comparator<T> this, @ReadOnly Object a1);
}
