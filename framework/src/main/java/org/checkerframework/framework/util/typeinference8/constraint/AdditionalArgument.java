package org.checkerframework.framework.util.typeinference8.constraint;

import com.sun.source.tree.ExpressionTree;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;

/**
 * A constraint that represents the bound set and the additional argument constraints generated from
 * a method or constructor invocation that is a part of a larger inference problem. When this
 * constraint is reduced it will generate more bounds and constraints from the invocation.
 *
 * <p>The larger inference problem creates this constraint only when it cannot create those bounds
 * and constraints right away. That happens when doing so needs the type of an implicit lambda
 * parameter for which the larger inference problem has not yet found a type; the bounds and
 * constraints cannot be created until after the implicit lambda parameter has a type.
 */
public class AdditionalArgument implements Constraint {

  /** The tree for the method or constructor invocation for this constraint. */
  private final ExpressionTree methodOrConstructorInvocation;

  /**
   * Creates a new constraint.
   *
   * @param methodOrConstructorInvocation tree for the method or constructor invocation for this
   *     constraint
   */
  public AdditionalArgument(ExpressionTree methodOrConstructorInvocation) {
    this.methodOrConstructorInvocation = methodOrConstructorInvocation;
  }

  @Override
  public Kind getKind() {
    return Kind.ADDITIONAL_ARG;
  }

  @Override
  public ReductionResult reduce(Java8InferenceContext context) {
    return context.inference.createArgConstraintsWithB2(methodOrConstructorInvocation);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Two {@code AdditionalArgument}s are equal if they are for the same invocation. Because
   * {@code JCTree} does not override {@code equals}, this compares the two trees by reference:
   * constraints for two textually identical invocations at different places in the source code are
   * not equal.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AdditionalArgument that = (AdditionalArgument) o;

    return methodOrConstructorInvocation.equals(that.methodOrConstructorInvocation);
  }

  @Override
  public int hashCode() {
    return methodOrConstructorInvocation.hashCode();
  }

  @Override
  public String toString() {
    return "AdditionalArgument: " + methodOrConstructorInvocation;
  }
}
