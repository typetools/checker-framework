package org.checkerframework.afu.scenelib.util;

/**
 * {@link EqualByStringRepresentation} is a "mix-in" class for objects that are equal if and only if
 * their {@code getClass} are equal and their {@link #toString toString} representations are equal.
 * {@link EqualByStringRepresentation} provides implementations of {@link #equals} and {@link
 * #hashCode} in terms of {@link #toString}.
 */
@SuppressWarnings("EqualsGetClass")
public abstract class EqualByStringRepresentation {
  @Override
  public abstract String toString();

  @Override
  public final boolean equals(Object that) {
    return that != null
        && this.getClass() == that.getClass()
        && this.toString().equals(that.toString());
  }

  @Override
  public final int hashCode() {
    return toString().hashCode();
  }
}
