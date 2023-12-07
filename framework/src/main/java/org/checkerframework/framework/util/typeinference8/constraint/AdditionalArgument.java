package org.checkerframework.framework.util.typeinference8.constraint;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import org.checkerframework.framework.util.typeinference8.types.InvocationType;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;

/**
 * A constraint the represent additional argument constraints generated from a method or constructor
 * invocation.
 */
public class AdditionalArgument implements Constraint {

  /** The tree for the method or constructor invocation for this constraint. */
  private ExpressionTree methodOrConstructorInvocation;

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
  public ConstraintSet reduce(Java8InferenceContext context) {
    if (methodOrConstructorInvocation.getKind() == Tree.Kind.METHOD_INVOCATION) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) methodOrConstructorInvocation;
      InvocationType methodType =
          context.inferenceTypeFactory.getTypeOfMethodAdaptedToUse(methodInvocation);
      Theta newMap =
          context.inferenceTypeFactory.createThetaForInvocation(
              methodInvocation, methodType, context);
      ConstraintSet set =
          context.inference.createC(methodType, methodInvocation.getArguments(), newMap);
      set.applyInstantiations();
      return set;
    } else {
      NewClassTree newClassTree = (NewClassTree) methodOrConstructorInvocation;
      InvocationType methodType =
          context.inferenceTypeFactory.getTypeOfMethodAdaptedToUse(newClassTree);

      Theta newMap =
          context.inferenceTypeFactory.createThetaForInvocation(newClassTree, methodType, context);
      ConstraintSet set =
          context.inference.createC(methodType, newClassTree.getArguments(), newMap);
      set.applyInstantiations();
      return set;
    }
  }
}
