package org.checkerframework.afu.scenelib.el;

import java.util.Objects;

/**
 * A {@link BoundLocation} holds location information for a bound of a type parameter of a class or
 * method: parameter index and bound index. It also handles type parameters themselves (not just the
 * bound part). It would be better named "TypeParameterLocation", or the two uses could be separated
 * out.
 */
public final class BoundLocation {
  /**
   * The index of the parameter to which the bound applies among all type parameters of the class or
   * method.
   */
  public final int paramIndex;

  /**
   * The index of the bound among all bounds on the type parameter. -1 if for the type parameter
   * itself.
   */
  public final int boundIndex;

  /**
   * Constructs a new {@link BoundLocation}; the arguments are assigned to the fields of the same
   * names.
   */
  public BoundLocation(int paramIndex, int boundIndex) {
    this.paramIndex = paramIndex;
    this.boundIndex = boundIndex;
  }

  /**
   * Returns true if this {@link BoundLocation} equals <code>o</code>; a slightly faster variant of
   * {@link #equals(Object)} for when the argument is statically known to be another nonnull {@link
   * BoundLocation}.
   */
  public boolean equals(BoundLocation l) {
    return paramIndex == l.paramIndex && boundIndex == l.boundIndex;
  }

  /**
   * This {@link BoundLocation} equals <code>o</code> if and only if <code>o</code> is another
   * nonnull {@link BoundLocation} and <code>this</code> and <code>o</code> have equal {@link
   * #paramIndex} and {@link #boundIndex}.
   */
  @Override
  public boolean equals(Object o) {
    return o instanceof BoundLocation && equals((BoundLocation) o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(paramIndex, boundIndex);
  }

  @Override
  public String toString() {
    return "BoundLocation(" + paramIndex + "," + boundIndex + ")";
  }
}
