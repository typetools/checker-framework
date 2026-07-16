package org.checkerframework.framework.util.typeinference8.constraint;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import org.checkerframework.framework.util.typeinference8.types.AbstractExecutableType;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;

/**
 * A constraint that represents additional argument constraints generated from a method or
 * constructor invocation that is a part of a larger inference problem. When this constraint is
 * reduced it will generate more constraints from the invocation. This is because creating the
 * constraints might use the type of an implicit lambda parameter for which the larger inference
 * problem has not yet found a type. So, the additional constraints cannot be created until after
 * the implicit lambda parameter has a type.
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
    if (methodOrConstructorInvocation instanceof MethodInvocationTree methodInvocation) {
      AbstractExecutableType executableType =
          context.inferenceTypeFactory.getTypeOfMethodAdaptedToUse(methodInvocation);
      Theta newMap =
          context.inferenceTypeFactory.createThetaForInvocation(
              methodInvocation, executableType, context);
      ConstraintSet set =
          context.inference.createC(executableType, methodInvocation.getArguments(), newMap);
      set.applyInstantiations();
      return set;
    } else {
      NewClassTree newClassTree = (NewClassTree) methodOrConstructorInvocation;
      AbstractExecutableType executableType =
          context.inferenceTypeFactory.getTypeOfMethodAdaptedToUse(newClassTree);

      Theta newMap =
          context.inferenceTypeFactory.createThetaForInvocation(
              newClassTree, executableType, context);
      ConstraintSet set =
          context.inference.createC(executableType, newClassTree.getArguments(), newMap);
      set.applyInstantiations();
      return set;
    }
  }
}
