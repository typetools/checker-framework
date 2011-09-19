package java.util;

import checkers.igj.quals.*;

@I
public interface Comparator<T> {
  public abstract int compare(@ReadOnly Comparator this, T a1, T a2);
  public abstract boolean equals(@ReadOnly Comparator this, @ReadOnly Object a1);
}
