package org.checkerframework.framework.util.typeinference8.constraint;

import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Constraints are between either an expression and a type, two types, or an expression and a thrown
 * type. Defined in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.1.2">JLS section
 * 18.1.2</a>
 */
public abstract class Constraint implements ReductionResult {

  public enum Kind {
    /**
     * {@code < Expression -> T >}: An expression is compatible in a loose invocation context with
     * type T
     */
    EXPRESSION,
    /** {@code < S -> T >}: A type S is compatible in a loose invocation context with type T */
    TYPE_COMPATIBILITY,
    /** {@code < S <: T >}: A reference type S is a subtype of a reference type T */
    SUBTYPE,
    /** {@code < S <= T >}: A type argument S is contained by a type argument T. */
    CONTAINED,
    /**
     * {@code < S = T >}: A type S is the same as a type T, or a type argument S is the same as type
     * argument T.
     */
    TYPE_EQUALITY,
    /**
     * {@code < LambdaExpression -> throws T>}: The checked exceptions thrown by the body of the
     * LambdaExpression are declared by the throws clause of the function type derived from T.
     */
    LAMBDA_EXCEPTION,
    /**
     * {@code < MethodReferenceExpression -> throws T>}: The checked exceptions thrown by the
     * referenced method are declared by the throws clause of the function type derived from T.
     */
    METHOD_REF_EXCEPTION,
  }

  /**
   * Return the kind of constraint.
   *
   * @return the kind of constraint
   */
  public abstract Kind getKind();

  /** T, the type on the right hand side of the constraint; may contain inference variables. */
  protected AbstractType T;

  protected Constraint(AbstractType T) {
    assert T != null : "Can't create a constraint with a null type.";
    this.T = T;
  }

  /**
   * Returns T which is the type on the right hand side of the constraint.
   *
   * @return T, that is the type on the right hand side of the constraint
   */
  public AbstractType getT() {
    return T;
  }

  /**
   * Reduce this constraint what this means depends on the kind of constraint. Reduction can produce
   * new bounds and/or new constraints.
   *
   * <p>Reduction is documented in <a
   * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.2">JLS section
   * 18.2</a>
   *
   * @param context Java8InferenceContext
   * @return the result of reducing this constraint
   */
  public abstract ReductionResult reduce(Java8InferenceContext context);

  /**
   * Returns a collection of all inference variables mentioned by this constraint.
   *
   * @return a collection of all inference variables mentioned by this constraint.
   */
  public Collection<Variable> getInferenceVariables() {
    return T.getInferenceVariables();
  }

  /**
   * For lambda and method references constraints, input variables are roughly the inference
   * variables mentioned by they function type's parameter types and return types. For conditional
   * expressions constraints, input variables are the union of the input variables of its
   * subexpressions. For all other constraints, no input variables exist.
   *
   * <p>Defined in <a
   * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2.2">JLS section
   * 18.5.2.2</a>
   *
   * @return input variables for this constraint
   */
  public abstract List<Variable> getInputVariables();

  /**
   * "The output variables of [expression] constraints are all inference variables mentioned by the
   * type on the right-hand side of the constraint, T, that are not input variables."
   *
   * <p>As defined in <a
   * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2.2">JLS section
   * 18.5.2.2</a>
   *
   * @return output variables for this constraint
   */
  public abstract List<Variable> getOutputVariables();

  /**
   * Implementation of {@link #getInputVariables()} that is used both by expressions constraints and
   * checked exception constraints
   * https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.5.2-200
   */
  protected List<Variable> getInputVariablesForExpression(ExpressionTree tree, AbstractType T) {

    switch (tree.getKind()) {
      case LAMBDA_EXPRESSION:
        if (T.isVariable()) {
          return Collections.singletonList((Variable) T);
        } else {
          LambdaExpressionTree lambdaTree = (LambdaExpressionTree) tree;
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
          for (ExpressionTree e : TreeUtils.getReturnedExpressions(lambdaTree)) {
            Constraint c = new Expression(e, R);
            inputs.addAll(c.getInputVariables());
          }
          return inputs;
        }
      case MEMBER_REFERENCE:
        if (T.isVariable()) {
          return Collections.singletonList((Variable) T);
        } else if (TreeUtils.isExactMethodReference((MemberReferenceTree) tree)) {
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
          return inputs;
        }
      case PARENTHESIZED:
        return getInputVariablesForExpression(TreeUtils.withoutParens(tree), T);
      case CONDITIONAL_EXPRESSION:
        ConditionalExpressionTree conditional = (ConditionalExpressionTree) tree;
        List<Variable> inputs = new ArrayList<>();
        inputs.addAll(getInputVariablesForExpression(conditional.getTrueExpression(), T));
        inputs.addAll(getInputVariablesForExpression(conditional.getFalseExpression(), T));
        return inputs;
      default:
        return Collections.emptyList();
    }
  }

  /**
   * Apply the given instantiations to any type mentioned in this constraint -- meaning replace any
   * mention of a variable in {@code instantiations} with its proper type.
   *
   * @param instantiations variables that have been instantiated
   */
  public void applyInstantiations(List<Variable> instantiations) {
    T = T.applyInstantiations(instantiations);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Constraint that = (Constraint) o;

    return T.equals(that.T);
  }

  @Override
  public int hashCode() {
    return T.hashCode();
  }
}
