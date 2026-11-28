package org.checkerframework.checker.nullness;

import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.HasParent;
import org.checkerframework.checker.nullness.qual.PathShapeBottom;
import org.checkerframework.checker.nullness.qual.UnknownPathShape;

/**
 * The path shape of a {@link java.nio.file.Path} or {@link java.io.File}: whether it has a name and
 * whether it has a parent.
 *
 * @see PathStringToPathShape
 */
public enum PathShape {
  // The constants are ordered from least to most specific.

  /** The path might be a root or the empty path. Corresponds to {@link UnknownPathShape}. */
  UNKNOWN,
  /** The path has a name. Corresponds to {@link HasName}. */
  HAS_NAME,
  /** The path has a parent and a name. Corresponds to {@link HasParent}. */
  HAS_PARENT,
  /** No path has this shape. Corresponds to {@link PathShapeBottom}. */
  BOTTOM;

  /**
   * Returns the less specific of this and {@code other}.
   *
   * @param other another path shape
   * @return the less specific of this and {@code other}
   */
  public PathShape lub(PathShape other) {
    return this.compareTo(other) <= 0 ? this : other;
  }

  /**
   * Returns the path shape that has the given properties. A path that has no name has no path shape
   * other than {@code UNKNOWN}, even if it has a parent.
   *
   * @param hasName true if a path has a non-empty name
   * @param hasParent true if the path has a parent
   * @return the path shape of the path
   */
  /*package-private*/ static PathShape of(boolean hasName, boolean hasParent) {
    if (!hasName) {
      return UNKNOWN;
    } else if (hasParent) {
      return HAS_PARENT;
    } else {
      return HAS_NAME;
    }
  }
}
