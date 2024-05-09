package org.checkerframework.dataflow.expression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

/**
 * The part of a <a
 * href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.13">Java Method
 * Reference expression</a> that follows "::". It can have the following forms:
 *
 * <ul>
 *   <li>{@literal [TypeArguments] Identifier}, which may be represented by a standard {@link
 *       JavaExpression}
 *   <li>{@literal [TypeArguments] "new"}, which is a constructor call and cannot be represented by
 *       an arbitrary {@link JavaExpression}
 * </ul>
 */
public class MethodReferenceTarget {

  /** The type arguments for this method reference target. */
  private final @Nullable JavaExpression typeArguments;

  /** The identifier for this method reference target. */
  private final @Nullable JavaExpression identifier;

  /** True if this method reference target is a constructor call. */
  private final boolean isConstructorCall;

  /**
   * Creates a new method reference target.
   *
   * @param typeArguments the type arguments
   * @param identifier the identifier
   * @param isConstructorCall whether a method reference target is a constructor call
   */
  public MethodReferenceTarget(
      @Nullable JavaExpression typeArguments,
      @Nullable JavaExpression identifier,
      boolean isConstructorCall) {
    this.typeArguments = typeArguments;
    this.identifier = identifier;
    this.isConstructorCall = isConstructorCall;
  }

  /**
   * Return the type arguments for this method reference target, or null if there are none.
   *
   * @return the type arguments for this method reference target
   */
  @Pure
  public @Nullable JavaExpression getTypeArguments() {
    return this.typeArguments;
  }

  /**
   * Return the identifier for this method reference target, or null if it's not an identifier (that
   * is, the method reference is for a constructor call).
   *
   * @return the identifier for this method reference target
   */
  @Pure
  public @Nullable JavaExpression getIdentifier() {
    return this.identifier;
  }

  /**
   * Return true if this method reference target is a constructor call (i.e., "new").
   *
   * @return true if this method reference target is a constructor call
   */
  public boolean isConstructorCall() {
    return this.isConstructorCall;
  }

  @Override
  @SuppressWarnings(
      "nullness:dereference.of.nullable") // If the target is not a constructor call, the identifier
  // is non-null
  public String toString() {
    String targetName = isConstructorCall() ? "new" : identifier.toString();
    if (typeArguments != null) {
      return typeArguments + targetName;
    }
    return targetName;
  }
}
