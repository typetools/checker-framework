package org.checkerframework.framework.util.typeinference8.constraint;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.UseOfVariable;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.TreeUtils;

/**
 * &lt;LambdaExpression &rarr;throws T&gt;: The checked exceptions thrown by the body of the
 * LambdaExpression are declared by the throws clause of the function type derived from T.
 *
 * <p>&lt;MethodReference &rarr;throws T&gt;: The checked exceptions thrown by the referenced method
 * are declared by the throws clause of the function type derived from T.
 */
public class CheckedExceptionConstraint extends TypeConstraint {

  /**
   * {@link com.sun.source.tree.LambdaExpressionTree} or {@link
   * com.sun.source.tree.MemberReferenceTree} for this constraint.
   */
  protected final ExpressionTree expression;

  /** The mapping from type variable to inference variable to use with this constraint. */
  protected final Theta map;

  /**
   * Creates a {@code CheckedExceptionConstraint}.
   *
   * @param expression {@link com.sun.source.tree.LambdaExpressionTree} or {@link
   *     com.sun.source.tree.MemberReferenceTree} for this constraint
   * @param t a function type
   * @param map The mapping from type variable to inference variable to use with this constraint
   */
  public CheckedExceptionConstraint(ExpressionTree expression, AbstractType t, Theta map) {
    super("Checked exception for " + expression, t);
    assert expression.getKind() == Tree.Kind.LAMBDA_EXPRESSION
        || expression.getKind() == Tree.Kind.MEMBER_REFERENCE;
    this.expression = expression;
    this.map = map;
  }

  @Override
  public Kind getKind() {
    return expression.getKind() == Tree.Kind.LAMBDA_EXPRESSION
        ? Kind.LAMBDA_EXCEPTION
        : Kind.METHOD_REF_EXCEPTION;
  }

  @Override
  public List<Variable> getInputVariables() {
    if (getKind() == Kind.LAMBDA_EXCEPTION) {
      if (T.isUseOfVariable()) {
        return Collections.singletonList(((UseOfVariable) T).getVariable());
      } else {
        LambdaExpressionTree lambdaTree = (LambdaExpressionTree) expression;
        List<Variable> inputs = new ArrayList<>();
        if (TreeUtils.isImplicitlyTypedLambda(lambdaTree)) {
          List<AbstractType> params = this.T.getFunctionTypeParameterTypes();
          if (params == null) {
            // T is not a function type.
            return Collections.emptyList();
          }
          for (AbstractType param : params) {
            inputs.addAll(param.getInferenceVariables());
          }
        }
        AbstractType R = this.T.getFunctionTypeReturnType();
        if (R == null || R.getTypeKind() == TypeKind.NONE) {
          return inputs;
        }
        inputs.addAll(R.getInferenceVariables());
        return inputs;
      }
    } else if (getKind() == Kind.METHOD_REF_EXCEPTION) {
      if (T.isUseOfVariable()) {
        return Collections.singletonList(((UseOfVariable) T).getVariable());
      } else if (TreeUtils.isExactMethodReference((MemberReferenceTree) expression)) {
        return Collections.emptyList();
      } else {
        List<AbstractType> params = this.T.getFunctionTypeParameterTypes();
        if (params == null) {
          // T is not a function type.
          return Collections.emptyList();
        }
        List<Variable> inputs = new ArrayList<>();
        for (AbstractType param : params) {
          inputs.addAll(param.getInferenceVariables());
        }
        AbstractType R = this.T.getFunctionTypeReturnType();
        if (R == null || R.getTypeKind() == TypeKind.NONE) {
          return inputs;
        }
        inputs.addAll(R.getInferenceVariables());
        return inputs;
      }
    }
    return getInputVariablesForExpression(expression, getT());
  }

  @Override
  public List<Variable> getOutputVariables() {
    List<Variable> input = getInputVariables();
    List<Variable> output = new ArrayList<>(getT().getInferenceVariables());
    output.removeAll(input);
    return output;
  }

  @Override
  public ReductionResult reduce(Java8InferenceContext context) {
    // See JLS 18.2.5
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
