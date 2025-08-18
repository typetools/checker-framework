package org.checkerframework.afu.scenelib.el;

import java.util.Objects;

/**
 * A {@link RelativeLocation} holds location information for a instanceof, cast, or new: either the
 * bytecode offset or the source code index. I call instanceof, cast, or new "the construct".
 */
public final class RelativeLocation implements Comparable<RelativeLocation> {
  /** The bytecode offset of the construct. */
  public final int offset;

  /** The source code index of the construct. */
  public final int index;

  /** The type index used for intersection types in casts. */
  public final int type_index;

  /**
   * Constructs a new {@link RelativeLocation}; the arguments are assigned to the fields of the same
   * names. Use -1 for the relative location that you're not using.
   */
  private RelativeLocation(int offset, int index, int type_index) {
    this.offset = offset;
    this.index = index;
    this.type_index = type_index;
  }

  public static RelativeLocation createOffset(int offset, int type_index) {
    return new RelativeLocation(offset, -1, type_index);
  }

  public static RelativeLocation createIndex(int index, int type_index) {
    return new RelativeLocation(-1, index, type_index);
  }

  public boolean isBytecodeOffset() {
    return offset > -1;
  }

  public String getLocationString() {
    if (isBytecodeOffset()) {
      return "#" + offset;
    } else {
      return "*" + index;
    }
  }

  @Override
  public int compareTo(RelativeLocation l) {
    int c = Integer.compare(offset, l.offset);
    if (c == 0) {
      c = Integer.compare(index, l.index);
      if (c == 0) {
        c = Integer.compare(type_index, l.type_index);
      }
    }
    return c;
  }

  /**
   * Returns true if this {@link RelativeLocation} equals {@code o}; a slightly faster variant of
   * {@link #equals(Object)} for when the argument is statically known to be another nonnull {@link
   * RelativeLocation}.
   */
  public boolean equals(RelativeLocation l) {
    return compareTo(l) == 0;
  }

  /**
   * This {@link RelativeLocation} equals {@code o} if and only if {@code o} is another nonnull
   * {@link RelativeLocation} and {@code this} and {@code o} have equal {@link #offset} and {@link
   * #index}.
   */
  @Override
  public boolean equals(Object o) {
    return o instanceof RelativeLocation && equals((RelativeLocation) o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(offset, index, type_index);
  }

  @Override
  public String toString() {
    return "RelativeLocation(" + getLocationString() + ")";
  }
}
