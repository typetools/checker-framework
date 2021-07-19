package org.checkerframework.framework.util.typeinference8.constraint;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;

/**
 * &lt;LambdaExpression &rarr;throws T&gt;: The checked exceptions thrown by the body of the
 * LambdaExpression are declared by the throws clause of the function type derived from T.
 *
 * <p>&lt;MethodReference &rarr;throws T&gt;: The checked exceptions thrown by the referenced method
 * are declared by the throws clause of the function type derived from T.
 */
public class CheckedExceptionConstraint extends Constraint {
  protected final ExpressionTree expression;
  protected final Theta map;

  public CheckedExceptionConstraint(ExpressionTree expression, AbstractType t, Theta map) {
    super(t);
    assert expression.getKind() == Tree.Kind.LAMBDA_EXPRESSION
        || expression.getKind() == Tree.Kind.MEMBER_REFERENCE;
    this.expression = expression;
    this.map = map;
  }

  public Theta getMap() {
    return map;
  }

  public ExpressionTree getExpression() {
    return expression;
  }

  @Override
  public Kind getKind() {
    return expression.getKind() == Tree.Kind.LAMBDA_EXPRESSION
        ? Kind.LAMBDA_EXCEPTION
        : Kind.METHOD_REF_EXCEPTION;
  }

  @Override
  public List<Variable> getInputVariables() {
    return getInputVariablesForExpression(expression, getT());
  }

  @Override
  public List<Variable> getOutputVariables() {
    List<Variable> input = getInputVariables();
    List<Variable> output = new ArrayList<>(getT().getInferenceVariables());
    output.removeAll(input);
    return output;
  }

  /** See JLS 18.2.5 */
  @Override
  public ReductionResult reduce(Java8InferenceContext context) {
    return context.inferenceTypeFactory.getCheckedExceptionConstraints(expression, T, map);
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

    CheckedExceptionConstraint that = (CheckedExceptionConstraint) o;

    return Objects.equals(expression, that.expression);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (expression != null ? expression.hashCode() : 0);
    return result;
  }
}
