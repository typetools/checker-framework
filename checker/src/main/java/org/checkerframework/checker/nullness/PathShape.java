package org.checkerframework.checker.nullness;

import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.HasParent;

/**
 * The path shape of a {@link java.nio.file.Path} or {@link java.io.File}: whether it has a name and
 * whether it has a parent. The constants are ordered from least to most specific, and each one
 * corresponds to a qualifier: {@code UNKNOWN} to {@link
 * org.checkerframework.checker.nullness.qual.UnknownPathShape}, {@code HAS_NAME} to {@link
 * HasName}, and {@code HAS_PARENT} to {@link HasParent}.
 *
 * @see PathStringClassifier
 */
public enum PathShape {
  /** The path might be a root or the empty path. */
  UNKNOWN,
  /** The path has a name. See {@link HasName}. */
  HAS_NAME,
  /** The path has a parent and a name. See {@link HasParent}. */
  HAS_PARENT;

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
   * Returns the path shape of {@code p.resolve(q)}, where this is the path shape of {@code p}.
   *
   * @param other the path shape of a path {@code q}
   * @return the path shape of {@code p.resolve(q)}
   */
  public PathShape resolve(PathShape other) {
    // If q is absolute, the result is q.  If q is empty, the result is p.  Otherwise, the result is
    // q appended to p.
    if (other == HAS_PARENT || (other == HAS_NAME && this != UNKNOWN)) {
      return HAS_PARENT;
    } else {
      return other;
    }
  }

  /**
   * Returns the path shape of {@code p.resolveSibling(q)}, where this is the path shape of {@code
   * p}.
   *
   * @param other the path shape of a path {@code q}
   * @return the path shape of {@code p.resolveSibling(q)}
   */
  public PathShape resolveSibling(PathShape other) {
    // The result is q if p has no parent, and p.getParent().resolve(q) otherwise.
    if (other == HAS_PARENT || (other == HAS_NAME && this == HAS_PARENT)) {
      return HAS_PARENT;
    } else {
      return other;
    }
  }

  /**
   * Returns the path shape of a path with the given properties. A path that has no name has no path
   * shape other than {@code UNKNOWN}, even if it has a parent.
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
