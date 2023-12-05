package org.checkerframework.framework.util.typeinference8.constraint;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import org.checkerframework.framework.util.typeinference8.types.InvocationType;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;

public class AdditionalArgument implements Constraint {
  ExpressionTree methodOrConstructorInvocation;

  public AdditionalArgument(ExpressionTree methodOrConstructorInvocation) {
    this.methodOrConstructorInvocation = methodOrConstructorInvocation;
  }

  @Override
  public Kind getKind() {
    return Kind.ADDITIONAL_ARG;
  }

  @Override
  public ReductionResult reduce(Java8InferenceContext context) {
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
