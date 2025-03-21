package org.checkerframework.framework.util.typeinference8.constraint;

import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.InferenceType;
import org.checkerframework.framework.util.typeinference8.types.InvocationType;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.SwitchExpressionScanner;
import org.checkerframework.javacutil.SwitchExpressionScanner.FunctionalSwitchExpressionScanner;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtils.MemberReferenceKind;
import org.plumelib.util.IPair;

/**
 * &lt;Expression &rarr; T&gt; An expression is compatible in a loose invocation context with type T
 */
public class Expression extends TypeConstraint {

  /** Expression that is compatible in a loose invocation context with {@link #T}. */
  private final ExpressionTree expression;

  /**
   * Creates an expression constraint.
   *
   * @param parent the constraint whose reduction created this constraint
   * @param expressionTree the expression for the constraint
   * @param t the type that the expression is compatible in a loose invocation context
   */
  public Expression(Constraint parent, ExpressionTree expressionTree, AbstractType t) {
    super(parent, t);
    assert expressionTree != null;
    this.expression = expressionTree;
  }

  /**
   * Creates an expression constraint.
   *
   * @param source a string describing where this constraint came from
   * @param expressionTree the expression for the constraint
   * @param t the type that the expression is compatible in a loose invocation context
   */
  public Expression(String source, ExpressionTree expressionTree, AbstractType t) {
    super(source, t);
    this.expression = expressionTree;
    assert expression != null;
  }

  @Override
  public Kind getKind() {
    return Kind.EXPRESSION;
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

  @Override
  public ReductionResult reduce(Java8InferenceContext context) {
    // See JLS 18.2.1
    if (getT().isProper()) {
      return reduceProperType();
    } else if (TreeUtils.isStandaloneExpression(expression)) {
      AbstractType s;
      if (!context.isLambdaParam(expression)) {
        s = new ProperType(expression, context);
      } else {
        AnnotatedTypeMirror atm = context.typeFactory.getAnnotatedType(expression);
        s = getT().create(atm, atm.getUnderlyingType());
      }
      return new Typing(this, s, T, TypeConstraint.Kind.TYPE_COMPATIBILITY);
    }
    switch (expression.getKind()) {
      case PARENTHESIZED:
        return new Expression(this, TreeUtils.withoutParens(expression), T);
      case NEW_CLASS:
      case METHOD_INVOCATION:
        return reduceMethodInvocation(context);
      case CONDITIONAL_EXPRESSION:
        ConditionalExpressionTree conditional = (ConditionalExpressionTree) expression;
        TypeConstraint trueConstraint = new Expression(this, conditional.getTrueExpression(), T);
        Constraint falseConstraint = new Expression(this, conditional.getFalseExpression(), T);
        return new ConstraintSet(trueConstraint, falseConstraint);
      case LAMBDA_EXPRESSION:
        return reduceLambda(context);
      case MEMBER_REFERENCE:
        return reduceMethodRef(context);
      default:
        if (TreeUtils.isSwitchExpression(expression)) {
          ConstraintSet set = new ConstraintSet();
          SwitchExpressionScanner<Void, Void> scanner =
              new FunctionalSwitchExpressionScanner<>(
                  (ExpressionTree valueTree, Void unused) -> {
                    Constraint c = new Expression(this, valueTree, T);
                    set.add(c);
                    return null;
                  },
                  (c1, c2) -> null);
          scanner.scanSwitchExpression(expression, null);
          return set;
        }
        throw new BugInCF(
            "Unexpected expression kind: %s, Expression: %s", expression.getKind(), expression);
    }
  }

  /**
   * JSL 18.2.1: "If T is a proper type, the constraint reduces to true if the expression is
   * compatible in a loose invocation context with T (5.3), and false otherwise."
   *
   * @return the result of reducing a proper type
   */
  private ReductionResult reduceProperType() {
    // Assume the constraint reduces to TRUE, if it did not the code wouldn't compile with
    // javac.

    // TODO: This should return false in some cases.
    // com.sun.tools.javac.code.Types.isConvertible(com.sun.tools.javac.code.Type,
    // com.sun.tools.javac.code.Type)
    return new ConstraintSet();
  }

  /**
   * Text from JLS 18.2.1: If the expression is a class instance creation expression or a method
   * invocation expression, the constraint reduces to the bound set B3 which would be used to
   * determine the expression's invocation type when targeting T, as defined in 18.5.2. (For a class
   * instance creation expression, the corresponding "method" used for inference is defined in
   * 15.9.3).
   *
   * <p>This bound set may contain new inference variables, as well as dependencies between these
   * new variables and the inference variables in T.
   *
   * @param context the context
   * @return the result of reducing this constraint
   */
  private BoundSet reduceMethodInvocation(Java8InferenceContext context) {
    ExpressionTree expressionTree = expression;
    List<? extends ExpressionTree> args;
    if (expressionTree.getKind() == Tree.Kind.NEW_CLASS) {
      NewClassTree newClassTree = (NewClassTree) expressionTree;
      args = newClassTree.getArguments();
    } else {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) expressionTree;
      args = methodInvocationTree.getArguments();
    }

    InvocationType methodType =
        context.inferenceTypeFactory.getTypeOfMethodAdaptedToUse(expressionTree);
    Theta map =
        context.inferenceTypeFactory.createThetaForInvocation(expressionTree, methodType, context);
    BoundSet b2 = context.inference.createB2(methodType, args, map);
    return context.inference.createB3(b2, expressionTree, methodType, T, map);
  }

  /**
   * Reduce this constraint
   *
   * @param context the context
   * @return the result of reducing this constraint
   */
  // https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.2.1-300
  private ReductionResult reduceMethodRef(Java8InferenceContext context) {
    MemberReferenceTree memRef = (MemberReferenceTree) expression;
    if (TreeUtils.isExactMethodReference(memRef)) {
      InvocationType typeOfPoAppMethod =
          context.inferenceTypeFactory.compileTimeDeclarationType(memRef);

      ConstraintSet constraintSet = new ConstraintSet();
      List<AbstractType> ps = T.getFunctionTypeParameterTypes();
      List<AbstractType> fs = typeOfPoAppMethod.getParameterTypes(null);

      if (ps.size() == fs.size() + 1) {
        AbstractType targetReference = ps.remove(0);
        ExpressionTree preColonTree = memRef.getQualifierExpression();
        AbstractType referenceType;
        if (context.isLambdaParam(preColonTree)) {
          AnnotatedTypeMirror atm = context.typeFactory.getAnnotatedType(preColonTree);
          referenceType = T.create(atm, atm.getUnderlyingType());
        } else {
          if (MemberReferenceKind.getMemberReferenceKind(memRef).isUnbound()) {
            AnnotatedTypeMirror atm =
                context.typeFactory.getAnnotatedTypeFromTypeTree(preColonTree);
            referenceType = new ProperType(atm, atm.getUnderlyingType(), context);
          } else {
            referenceType = new ProperType(preColonTree, context);
          }
        }
        constraintSet.add(
            new Typing(this, targetReference, referenceType, TypeConstraint.Kind.SUBTYPE));
      }
      for (int i = 0; i < ps.size(); i++) {
        constraintSet.add(new Typing(this, ps.get(i), fs.get(i), TypeConstraint.Kind.SUBTYPE));
      }
      AbstractType r = T.getFunctionTypeReturnType();
      if (r != null && r.getTypeKind() != TypeKind.VOID) {
        AbstractType rPrime = typeOfPoAppMethod.getReturnType(null).capture(context);
        constraintSet.add(new Typing(this, rPrime, r, TypeConstraint.Kind.TYPE_COMPATIBILITY));
      }
      return constraintSet;
    }
    // else the method reference is inexact.

    // Compile-time declaration of the member reference expression
    InvocationType compileTimeDecl =
        context.inferenceTypeFactory.compileTimeDeclarationType(memRef);
    if (compileTimeDecl.isVoid()) {
      return ConstraintSet.TRUE;
    }
    AbstractType r = T.getFunctionTypeReturnType();
    if (r.getTypeKind() == TypeKind.VOID) {
      return ConstraintSet.TRUE;
    }

    // https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.2.1-300-D-B-BC
    // Otherwise, if the method reference expression elides TypeArguments, and the
    // compile-time declaration is a generic method, and
    // the return type of the compile-time declaration mentions at least one of the method's
    // type parameters, the constraint reduces to the bound set B3 which would be used to
    // determine the method reference's invocation type when targeting the return type of the
    // function type, as defined in 18.5.2. B3 may contain new inference variables, as well as
    // dependencies between these new variables and the inference variables in T.
    Theta map =
        context.inferenceTypeFactory.createThetaForMethodReference(
            memRef, compileTimeDecl, context);
    AbstractType compileTimeReturn = compileTimeDecl.getReturnType(map);
    if (TreeUtils.needsTypeArgInference(memRef) && !compileTimeReturn.isProper()) {
      BoundSet b2 =
          context.inference.createB2MethodRef(
              compileTimeDecl, T.getFunctionTypeParameterTypes(), map);
      return context.inference.createB3(b2, memRef, compileTimeDecl, r, map);
    }

    // https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.2.1-300-D-B-C
    // Otherwise, let R be the return type of the function type, and let R' be the result
    // of applying capture conversion (5.1.10) to the return type of the invocation type
    // (15.12.2.6) of the compile-time declaration. If R' is void, the constraint reduces
    // to false; otherwise, the constraint reduces to <R' -> R>.
    return ReductionResultPair.of(
        new ConstraintSet(
            new Typing(
                this,
                compileTimeReturn.capture(context),
                r,
                TypeConstraint.Kind.TYPE_COMPATIBILITY)),
        new BoundSet(context));
  }

  /**
   * Reduce this constraint
   *
   * @param context the context
   * @return the result of reducing this constraint
   */
  // See https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.2.1-200
  private ReductionResultPair reduceLambda(Java8InferenceContext context) {
    LambdaExpressionTree lambda = (LambdaExpressionTree) expression;
    IPair<AbstractType, BoundSet> pair = getGroundTargetType(T, lambda, context);
    AbstractType tPrime = pair.first;
    BoundSet boundSet = pair.second == null ? new BoundSet(context) : pair.second;

    ConstraintSet constraintSet = new ConstraintSet();

    if (!TreeUtils.isImplicitlyTypedLambda(lambda)) {
      // Explicitly typed lambda
      List<? extends VariableTree> parameters = lambda.getParameters();
      List<AbstractType> gs = T.getFunctionTypeParameterTypes();
      assert parameters.size() == gs.size();

      for (int i = 0; i < gs.size(); i++) {
        VariableTree parameter = parameters.get(i);
        AbstractType fi = new ProperType(parameter, context);
        AbstractType gi = gs.get(i);
        constraintSet.add(new Typing(this, fi, gi, TypeConstraint.Kind.TYPE_EQUALITY));
      }
      @SuppressWarnings("interning:not.interned") // checking for exact object.
      boolean tPrimeNotSameAsT = tPrime != T;
      if (tPrimeNotSameAsT) {
        constraintSet.add(new Typing(this, tPrime, T, TypeConstraint.Kind.SUBTYPE));
      }
    } else {
      context.addLambdaParms(lambda.getParameters());
    }

    AbstractType R = tPrime.getFunctionTypeReturnType();
    if (R != null && R.getTypeKind() != TypeKind.VOID) {
      for (ExpressionTree e : TreeUtils.getReturnedExpressions(lambda)) {
        if (R.isProper()) {
          if (!context.env.getTypeUtils().isAssignable(TreeUtils.typeOf(e), R.getJavaType())) {
            boundSet.addFalse();
            return ReductionResultPair.of(constraintSet, boundSet);
          }
        } else {
          constraintSet.add(new Expression(this, e, R));
        }
      }
    }
    return ReductionResultPair.of(constraintSet, boundSet);
  }

  /**
   * This method sets up functional interface parameterization inference for {@code lambda} as
   * defined in JLS 18.5.3.
   *
   * <p>Computes the ground target type of {@code t}. Returned as the first in the pair. This
   * process might create additional bounds, if so the second in the returned pair will be non-null.
   *
   * @param t the target type of {@code lambda}
   * @param lambda a lambda to infer functional interface parameterization
   * @param context the context
   * @return the ground target type
   */
  private IPair<AbstractType, BoundSet> getGroundTargetType(
      AbstractType t, LambdaExpressionTree lambda, Java8InferenceContext context) {
    if (!t.isWildcardParameterizedType()) {
      return IPair.of(t, null);
    }
    // 15.27.3:
    // If T is a wildcard-parameterized functional interface type and the lambda expression is
    // explicitly typed, then the ground target type is inferred as described in 18.5.3.
    if (TreeUtils.isExplicitlyTypeLambda(lambda)) {
      return explicitlyTypedLambdaWithWildcard(t, lambda, context);
    } else {
      // If T is a wildcard-parameterized functional interface type and the lambda expression
      // is implicitly typed, then the ground target type is the non-wildcard parameterization
      // (9.9) of T.
      // https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-9.9-200-C
      return IPair.of(nonWildcardParameterization(t, context), null);
    }
  }

  /**
   * Returns the non-wildcard parameterization of {@code t} as defined in JLS 9.9.
   *
   * @param t a type
   * @param context the context
   * @return the non-wildcard parameterization of {@code t}
   */
  private AbstractType nonWildcardParameterization(AbstractType t, Java8InferenceContext context) {
    List<AbstractType> As = t.getTypeArguments();
    Iterator<ProperType> Bs = t.getTypeParameterBounds().iterator();
    List<AbstractType> Ts = new ArrayList<>();
    for (AbstractType Ai : As) {
      ProperType bi = Bs.next();
      if (Ai.getTypeKind() != TypeKind.WILDCARD) {
        Ts.add(Ai);
      } else if (Ai.isUnboundWildcard()) {
        Ts.add(bi);
      } else if (Ai.isUpperBoundedWildcard()) {
        AbstractType Ui = Ai.getWildcardUpperBound();
        AbstractType glb = context.inferenceTypeFactory.glb(Ui, bi);
        Ts.add(glb);
      } else {
        // Lower bounded wildcard
        Ts.add(Ai.getWildcardLowerBound());
      }
    }
    return t.replaceTypeArgs(Ts);
  }

  /**
   * Infers the type of {@code lambda} which may create a bounds set that needs to be resolved as
   * part of a larger inference problem. See 18.5.3: Functional Interface Parameterization Inference
   *
   * @param t the target type of the lambda
   * @param lambda a lambda expression
   * @param context the context
   * @return a pair of the type of the lambda and the bound set that needs to be resolved
   */
  private IPair<AbstractType, BoundSet> explicitlyTypedLambdaWithWildcard(
      AbstractType t, LambdaExpressionTree lambda, Java8InferenceContext context) {
    // Where a lambda expression with explicit parameter types P1, ..., Pn targets a functional
    // interface type F<A1, ..., Am> with at least one wildcard type argument, then a
    // parameterization of F may be derived as the ground target type of the lambda expression
    // as follows.
    List<ProperType> ps = new ArrayList<>();
    for (VariableTree paramTree : lambda.getParameters()) {
      ps.add(new ProperType(paramTree, context));
    }

    // Let Q1, ..., Qk be the parameter types of the function type of the type F<alpha1, ...,
    // alpham>, where alpha1, ..., alpham are fresh inference variables.
    Theta map = context.inferenceTypeFactory.createThetaForLambda(lambda, t);
    List<Variable> alphas = new ArrayList<>(map.values());
    AbstractType tprime = InferenceType.create(t.getAnnotatedType(), t.getJavaType(), map, context);

    List<AbstractType> qs = tprime.getFunctionTypeParameterTypes();
    assert qs.size() == ps.size();

    // A set of constraint formulas is formed with, for all i (1 <= i <= n), <Pi = Qi>.
    ConstraintSet constraintSet = new ConstraintSet();
    for (int i = 0; i < ps.size(); i++) {
      ProperType pi = ps.get(i);
      AbstractType qi = qs.get(i);
      constraintSet.add(new Typing(this, pi, qi, TypeConstraint.Kind.TYPE_EQUALITY));
    }
    // This constraint formula set is reduced to form the bound set B.
    BoundSet b = constraintSet.reduce(context);
    assert !b.containsFalse()
        : "Bound set contains false during Functional Interface Parameterization Inference";

    // A new parameterization of the functional interface type, F<A'1, ..., A'm>, is constructed
    // as follows, for 1 <= i <= m:
    List<AbstractType> APrimes = new ArrayList<>();
    Iterator<Variable> alphaIter = alphas.iterator();
    boolean hasWildcard = false;
    for (AbstractType Ai : t.getTypeArguments()) {
      Variable alphaI = alphaIter.next();
      // If B contains an instantiation (18.1.3) for alphai, T, then A'i = T.
      AbstractType AiPrime = alphaI.getBounds().getInstantiation();
      if (AiPrime == null) {
        AiPrime = Ai;
      }
      APrimes.add(AiPrime);
      if (AiPrime.getTypeKind() == TypeKind.WILDCARD) {
        hasWildcard = true;
      }
    }

    // The inferred parameterization is either F<A'1, ..., A'm>, if all the type arguments
    // are types, or the non-wildcard parameterization (9.9) of F<A'1, ..., A'm>, if one or more
    // type arguments are still wildcards.

    AbstractType target = t.replaceTypeArgs(APrimes);
    if (hasWildcard) {
      return IPair.of(nonWildcardParameterization(target, context), b);
    }
    return IPair.of(target, b);
  }

  @Override
  public String toString() {
    return expression + " -> " + T;
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

    Expression that = (Expression) o;

    return expression.equals(that.expression);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + expression.hashCode();
    return result;
  }
}
