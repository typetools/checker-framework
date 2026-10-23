package org.checkerframework.framework.type;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Represents upper and lower bounds, each an AnnotatedTypeMirror. */
public class AnnotatedTypeParameterBounds {
  private final AnnotatedTypeMirror upper;
  private final AnnotatedTypeMirror lower;

  public AnnotatedTypeParameterBounds(AnnotatedTypeMirror upper, AnnotatedTypeMirror lower) {
    this.upper = upper;
    this.lower = lower;
  }

  public AnnotatedTypeMirror getUpperBound() {
    return upper;
  }

  public AnnotatedTypeMirror getLowerBound() {
    return lower;
  }

  @Override
  public String toString() {
    return "[extends " + upper + " super " + lower + "]";
  }

  /**
   * Returns a possibly-verbose string representation of this.
   *
   * @param verbose if true, returned representation is verbose
   * @return a possibly-verbose string representation of this
   */
  public String toString(boolean verbose) {
    return "[extends " + upper.toString(verbose) + " super " + lower.toString(verbose) + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hash(upper, lower);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof AnnotatedTypeParameterBounds other)) {
      return false;
    }
    return Objects.equals(this.upper, other.upper) && Objects.equals(this.lower, other.lower);
  }
}
