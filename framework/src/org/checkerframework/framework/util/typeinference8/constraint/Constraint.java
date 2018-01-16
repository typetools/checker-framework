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
import org.checkerframework.framework.util.typeinference8.util.Context;
import org.checkerframework.javacutil.TreeUtils;

/** https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.1.2 */
public abstract class Constraint implements ReductionResult {

    public enum Kind {
        /**
         * {@code < Expression -> T >}: An expression is compatible in a loose invocation context
         * with type T
         */
        EXPRESSION,
        /** {@code < S -> T >}: A type S is compatible in a loose invocation context with type T */
        TYPE_COMPATIBILITY,
        /** {@code < S <: T >}: A reference type S is a subtype of a reference type T */
        SUBTYPE,
        /** {@code < S <= T >}: A type argument S is contained by a type argument T. */
        CONTAINED,
        /**
         * {@code < S = T >}: A type S is the same as a type T, or a type argument S is the same as
         * type argument T.
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
    /** T: may contain inference variables. */
    public AbstractType T;

    protected Constraint(AbstractType t) {
        assert t != null : "Can't create a constraint with a null type.";
        T = t;
    }

    public AbstractType getT() {
        return T;
    }

    public abstract ReductionResult reduce(Context context);

    /** https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.5.2-200 */
    protected List<Variable> getInputVariablesForExpression(ExpressionTree tree, AbstractType t) {

        switch (tree.getKind()) {
            case LAMBDA_EXPRESSION:
                if (t.isVariable()) {
                    return Collections.singletonList((Variable) t);
                } else {
                    LambdaExpressionTree lambdaTree = (LambdaExpressionTree) tree;
                    List<Variable> inputs = new ArrayList<>();
                    if (TreeUtils.isImplicitlyTypeLambda(lambdaTree)) {
                        List<AbstractType> params = T.getFunctionTypeParameterTypes();
                        if (params == null) {
                            // T is not a function type.
                            return Collections.emptyList();
                        }
                        for (AbstractType param : params) {
                            inputs.addAll(param.getInferenceVariables());
                        }
                    }
                    AbstractType R = T.getFunctionTypeReturnType();
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
                if (t.isVariable()) {
                    return Collections.singletonList((Variable) t);
                } else if (TreeUtils.isExactMethodReference((MemberReferenceTree) tree)) {
                    return Collections.emptyList();
                } else {
                    List<AbstractType> params = T.getFunctionTypeParameterTypes();
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
                return getInputVariablesForExpression(TreeUtils.skipParens(tree), t);
            case CONDITIONAL_EXPRESSION:
                ConditionalExpressionTree conditional = (ConditionalExpressionTree) tree;
                List<Variable> inputs = new ArrayList<>();
                inputs.addAll(getInputVariablesForExpression(conditional.getTrueExpression(), t));
                inputs.addAll(getInputVariablesForExpression(conditional.getFalseExpression(), t));
                return inputs;
            default:
                return Collections.emptyList();
        }
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

    public abstract Kind getKind();

    public Collection<Variable> getInferenceVariables() {
        return T.getInferenceVariables();
    }

    public abstract List<Variable> getInputVariables();

    public abstract List<Variable> getOutputVariables();

    public void applyInstantiations(List<Variable> instantiations) {
        T = T.applyInstantiations(instantiations);
    }
}
