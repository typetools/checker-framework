package org.checkerframework.framework.util.typeinference8.constraint;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.UseOfVariable;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The additional argument constraints (<a
 * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-18.html#jls-18.5.2.2">JLS
 * 18.5.2.2</a>) produced by the body of an implicitly typed lambda expression, deferred until the
 * lambda's parameter types are known.
 *
 * <p>Creating those constraints requires the type of each invocation in the lambda body, and
 * therefore the types of the lambda's parameters. For an implicitly typed lambda, a parameter's
 * type is the corresponding parameter type of the function type derived from the target type {@code
 * T}; while that type still mentions an inference variable, the parameter has no type yet. JLS
 * 18.2.1 says that a constraint on an implicitly typed lambda whose function type has a parameter
 * type that is not a proper type "reduces to false", and notes that the condition "never arises in
 * practice" precisely because 18.5.2.2 resolves the constraint's input variables first.
 *
 * <p>This constraint is the placeholder that makes that ordering happen: its input variables are
 * the inference variables that the lambda's parameter types mention, so {@link
 * org.checkerframework.framework.util.typeinference8.InvocationTypeInference#getB4} resolves them
 * before reducing it, and reducing it then creates the body's constraints from parameter types that
 * are proper.
 */
public class LambdaBodyConstraint extends TypeConstraint {

  /** The implicitly typed lambda whose body's constraints are deferred. */
  private final LambdaExpressionTree lambda;

  /**
   * Creates a {@code LambdaBodyConstraint}.
   *
   * @param lambda an implicitly typed lambda expression
   * @param t the target type of {@code lambda}
   */
  public LambdaBodyConstraint(LambdaExpressionTree lambda, AbstractType t) {
    super("Deferred lambda body constraints for " + lambda, t);
    this.lambda = lambda;
  }

  /**
   * Returns whether the constraints produced by the body of {@code lambda} must be deferred,
   * because the lambda's parameters do not have types yet.
   *
   * <p>This is the case when {@code lambda} is implicitly typed and either the target type {@code
   * t} is itself an inference variable, or the function type derived from {@code t} has a parameter
   * type that is not a proper type.
   *
   * @param lambda a lambda expression that is an argument of an invocation under inference
   * @param t the target type of {@code lambda}
   * @return whether the constraints produced by the body of {@code lambda} must be deferred
   */
  public static boolean mustDefer(LambdaExpressionTree lambda, AbstractType t) {
    if (!TreeUtils.isImplicitlyTypedLambda(lambda)) {
      // An explicitly typed lambda's parameters have types no matter what inference does.
      return false;
    }
    if (t.isUseOfVariable()) {
      // No function type can be derived from an inference variable.
      return true;
    }
    List<AbstractType> params = t.getFunctionTypeParameterTypes();
    if (params == null) {
      // No function type can be derived from t at all; deferring would not help.
      return false;
    }
    for (AbstractType param : params) {
      if (!param.isProper()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Kind getKind() {
    return Kind.LAMBDA_BODY;
  }

  /**
   * {@inheritDoc}
   *
   * <p>The input variables are those that make the lambda's parameter types improper: the target
   * type itself if it is an inference variable, and otherwise the inference variables mentioned by
   * the function type's parameter types. These are the input variables that JLS 18.5.2.2 defines
   * for the corresponding {@code <LambdaExpression -> T>} constraint, restricted to the ones that
   * the lambda's parameters depend on.
   */
  @Override
  public List<Variable> getInputVariables() {
    if (T.isUseOfVariable()) {
      return Collections.singletonList(((UseOfVariable) T).getVariable());
    }
    List<Variable> paramVariables = functionTypeParameterVariables();
    if (paramVariables == null) {
      // T is not a function type.
      return Collections.emptyList();
    }
    return paramVariables;
  }

  @Override
  public List<Variable> getOutputVariables() {
    List<Variable> input = getInputVariables();
    List<Variable> output = new ArrayList<>(getT().getInferenceVariables());
    output.removeAll(input);
    return output;
  }

  /**
   * {@inheritDoc}
   *
   * <p>The lambda's parameter types are proper by now, so the body's additional argument
   * constraints can be created.
   */
  @Override
  public ReductionResult reduce(Java8InferenceContext context) {
    ConstraintSet constraintSet = new ConstraintSet();
    for (ExpressionTree returnedExpression : TreeUtils.getReturnedExpressions(lambda)) {
      constraintSet.addAll(
          context.inference.createAdditionalArgConstraintsNoLambda(returnedExpression));
    }
    return constraintSet;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    LambdaBodyConstraint that = (LambdaBodyConstraint) o;

    return Objects.equals(lambda, that.lambda);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), lambda);
  }
}
