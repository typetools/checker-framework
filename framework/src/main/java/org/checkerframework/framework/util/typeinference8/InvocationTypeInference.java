package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;
import org.checkerframework.framework.util.typeinference8.bound.CaptureBound;
import org.checkerframework.framework.util.typeinference8.constraint.AdditionalArgument;
import org.checkerframework.framework.util.typeinference8.constraint.CheckedExceptionConstraint;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint.Kind;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.Expression;
import org.checkerframework.framework.util.typeinference8.constraint.TypeConstraint;
import org.checkerframework.framework.util.typeinference8.constraint.Typing;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.InferenceType;
import org.checkerframework.framework.util.typeinference8.types.InvocationType;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.UseOfVariable;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.FalseBoundException;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Resolution;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.SwitchExpressionScanner;
import org.checkerframework.javacutil.SwitchExpressionScanner.FunctionalSwitchExpressionScanner;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Performs invocation type inference as described in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2">JLS Section
 * 18.5.2</a>. Main entry point is {@link InvocationTypeInference#infer(ExpressionTree,
 * AnnotatedExecutableType)}
 *
 * <p>Invocation type inference is the process by which method type arguments are inferred for a
 * given method invocation. An overview of the process is given below.
 *
 * <p>1. Inference creates an inference variable for each method type argument for a given method
 * invocation. Each inference variable may have zero or more upper, lower, and equal bounds. The
 * bounds of an inference variable are initially the bounds on the type argument. More bounds may be
 * infered in later steps.
 *
 * <p>Bounds are between an inference variable and an abstract type. {@link AbstractType}s are
 * type-like structures that might include inference variables. Abstract types might also be an
 * inference variable or a type without any inference variables, which is also know as a proper
 * type.
 *
 * <p>An inference variable is represented by a {@link Variable} object which holds bounds for the
 * inference variable, in an {@link
 * org.checkerframework.framework.util.typeinference8.types.VariableBounds} object. Additional
 * inference variables may be created in later steps if any subexpression of the method invocation
 * requires type inference.
 *
 * <p>2. Next, inference creates constraints between the arguments to the method invocation and its
 * formal parameters. Also, for non-void methods, a constraint between the declared return type and
 * the "target type" of the method invocation is created. "Target types" are defined in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html">JLS Chapter 5</a>. For
 * example, the target type of a method invocation assigned to a variable is the type of the
 * variable.
 *
 * <p>Constraints are represented by {@link TypeConstraint} objects and are between abstract types
 * (see {@link AbstractType}) and either expressions (see {@link Expression}) or other abstract
 * types. A constraint might also be an abstract type that might be thrown by the method invocation
 * (see {@link CheckedExceptionConstraint}). Groups of constraints are stored in {@link
 * ConstraintSet}s.
 *
 * <p>3. Next, these constraints are "reduced" producing bounds on the inference variables.
 * Reduction depends on the kind of constraint and is defined in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.2">JLS section
 * 18.2</a>. In this code base, constraints are reduced via {@link
 * ConstraintSet#reduce(Java8InferenceContext)}.
 *
 * <p>4. The inference variables' bounds are then "incorporated" which produces more bounds and/or
 * constraints that must then be "reduced" or "incorporated". Incorporation and reduction continue
 * until no new bounds or constraints are produced. Bounds are incorporated via {@link
 * BoundSet#incorporateToFixedPoint(BoundSet)}. Incorporation in defined in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.3">JLS section
 * 18.3</a>.
 *
 * <p>5. Finally, a type for each inference variable is computed by "resolving" the bounds.
 * Variables are resolved via {@link Resolution#resolve(Collection, BoundSet,
 * Java8InferenceContext)}. Resolution is defined in the <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.4">JLS section
 * 18.4</a>.
 *
 * <p>An object of this class stores information about some particular invocation that requires
 * inference.
 */
public class InvocationTypeInference {

  /** Checker used to issue errors/warnings. */
  protected final SourceChecker checker;

  /** Stores information about the current inference problem being solved. */
  protected final Java8InferenceContext context;

  /** Tree for which type arguments are being inferred. */
  protected final Tree inferenceExpression;

  /**
   * Creates an inference problem.
   *
   * @param factory the annotated type factory to use
   * @param pathToExpression path to the expression for which inference is preformed
   */
  public InvocationTypeInference(AnnotatedTypeFactory factory, TreePath pathToExpression) {
    this.checker = factory.getChecker();
    this.context = new Java8InferenceContext(factory, pathToExpression, this);
    this.inferenceExpression = pathToExpression.getLeaf();
  }

  /**
   * Returns the tree for which inference is being inferred.
   *
   * @return the tree for which inference is being inferred
   */
  public Tree getInferenceExpression() {
    return inferenceExpression;
  }

  /**
   * Perform invocation type inference on {@code invocation}. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2">JLS
   * 18.5.2</a>.
   *
   * @param invocation invocation which needs inference
   * @param methodType type of the method invocation
   * @return the result of inference
   * @throws FalseBoundException if inference fails because of the java types
   */
  public InferenceResult infer(ExpressionTree invocation, AnnotatedExecutableType methodType)
      throws FalseBoundException {
    ExecutableType e = methodType.getUnderlyingType();
    InvocationType invocationType = new InvocationType(methodType, e, invocation, context);
    ProperType target = context.inferenceTypeFactory.getTargetType();
    List<? extends ExpressionTree> args;
    if (invocation.getKind() == Tree.Kind.METHOD_INVOCATION) {
      args = ((MethodInvocationTree) invocation).getArguments();
    } else {
      args = ((NewClassTree) invocation).getArguments();
    }

    Theta map =
        context.inferenceTypeFactory.createThetaForInvocation(invocation, invocationType, context);
    BoundSet b2 = createB2(invocationType, args, map);
    BoundSet b3;
    if (target != null && TreeUtils.isPolyExpression(invocation)) {
      b3 = createB3(b2, invocation, invocationType, target, map);
    } else {
      b3 = b2;
    }
    ConstraintSet c = createC(invocationType, args, map);

    BoundSet b4 = getB4(b3, c);
    b4.resolve();
    return new InferenceResult(
        b4.getInstantiatedVariables(),
        b4.isUncheckedConversion(),
        b4.annoInferenceFailed,
        b4.errorMsg);
  }

  /**
   * Perform invocation type inference on {@code invocation}. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2">JLS
   * 18.5.2</a>.
   *
   * @param invocation member reference tree
   * @return the result of inference
   * @throws FalseBoundException if inference fails because of the java types
   */
  public InferenceResult infer(MemberReferenceTree invocation) throws FalseBoundException {

    ProperType target = context.inferenceTypeFactory.getTargetType();
    AbstractType target1 =
        InferenceType.create(
            target.getAnnotatedType(),
            target.getJavaType(),
            context.maps.get(context.pathToExpression.getParentPath().getLeaf()),
            context);
    target = (ProperType) target1.applyInstantiations();
    if (target == null) {
      throw new BugInCF("Target of method reference should not be null: %s", invocation);
    }

    InvocationType compileTimeDecl =
        context.inferenceTypeFactory.compileTimeDeclarationType(invocation);
    Theta map =
        context.inferenceTypeFactory.createThetaForMethodReference(
            invocation, compileTimeDecl, context);
    BoundSet b2 = createB2MethodRef(compileTimeDecl, target.getFunctionTypeParameterTypes(), map);
    AbstractType r = target.getFunctionTypeReturnType();
    BoundSet b3;
    if (r == null || r.getTypeKind() == TypeKind.VOID) {
      b3 = b2;
    } else {
      b3 = createB3(b2, invocation, compileTimeDecl, r, map);
    }

    List<Variable> thetaPrime = b3.resolve();

    return new InferenceResult(
        thetaPrime, b3.isUncheckedConversion(), b3.annoInferenceFailed, b3.errorMsg);
  }

  /**
   * Creates the bound set used to determine whether a method is applicable. This method is called
   * B2 in <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.1">JLS
   * Section 18.5.1</a>.
   *
   * <p>It does this by:
   *
   * <ol>
   *   <li value="1">Creating the inference variables and initializing their bounds based on the
   *       type parameter declaration.
   *   <li value="2">Adding any bounds implied by the throws clause of {@code methodType}.
   *   <li value="3">Constructing constraints between formal parameters and arguments that are
   *       "pertinent to applicability" (See <a
   *       href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.12.2.2">JLS
   *       Section 15.12.2.2</a>). Generally, all arguments are applicable except: inexact method
   *       reference, implicitly typed lambdas, or explicitly typed lambda whose return
   *       expression(s) are not pertinent.
   *   <li value="4">Reducing and incorporating those constraints which finally produces B2.
   * </ol>
   *
   * @param methodType the type of the method or constructor invoked
   * @param args argument expression tress
   * @param map map of type variables to (inference) variables
   * @return bound set used to determine whether a method is applicable
   */
  public BoundSet createB2(
      InvocationType methodType, List<? extends ExpressionTree> args, Theta map) {
    BoundSet b0 = BoundSet.initialBounds(map, context);

    // For all i (1 <= i <= p), if Pi appears in the throws clause of m, then the bound throws
    // alphai is implied. These bounds, if any, are incorporated with B0 to produce a new bound
    // set, B1.
    for (AbstractType thrownType : methodType.getThrownTypes(map)) {
      if (thrownType.isUseOfVariable()) {
        ((UseOfVariable) thrownType).setHasThrowsBound(true);
      }
    }

    BoundSet b1 = b0;
    ConstraintSet c = new ConstraintSet();
    List<AbstractType> formals = methodType.getParameterTypes(map, args.size());

    for (int i = 0; i < formals.size(); i++) {
      ExpressionTree ei = args.get(i);
      AbstractType fi = formals.get(i);

      if (!notPertinentToApplicability(ei, fi)) {
        c.add(new Expression(ei, fi));
      }
    }

    BoundSet newBounds = c.reduce(context);
    assert !newBounds.containsFalse();
    b1.incorporateToFixedPoint(newBounds);

    return b1;
  }

  /**
   * Same as {@link #createB2(InvocationType, List, Theta)}, but for method references. A list of
   * types is used instead of a list of arguments. These types are the types of the formal
   * parameters of function type of target type of the method reference.
   *
   * @param methodType the type of the method or constructor invoked
   * @param args types to use as arguments
   * @param map map of type variables to (inference) variables
   * @return bound set used to determine whether a method is applicable
   */
  public BoundSet createB2MethodRef(InvocationType methodType, List<AbstractType> args, Theta map) {
    BoundSet b0 = BoundSet.initialBounds(map, context);

    // For all i (1 <= i <= p), if Pi appears in the throws clause of m, then the bound throws
    // alphai is implied. These bounds, if any, are incorporated with B0 to produce a new bound
    // set, B1.
    for (AbstractType thrownType : methodType.getThrownTypes(map)) {
      if (thrownType.isUseOfVariable()) {
        ((UseOfVariable) thrownType).setHasThrowsBound(true);
      }
    }

    BoundSet b1 = b0;
    ConstraintSet c = new ConstraintSet();
    List<AbstractType> formals = methodType.getParameterTypes(map, args.size());
    if (TreeUtils.isLikeDiamondMemberReference(methodType.getInvocation())) {
      // https://docs.oracle.com/javase/specs/jls/se19/html/jls-15.html#jls-15.13.1
      //  If ReferenceType is a raw type, and there exists a parameterization of this type,
      // G<...>, that is a supertype of P1, the type to search is the result of capture
      // conversion (§5.1.10) applied to G<...>; otherwise, the type to search is the same
      // as the type of the first search. Type arguments, if any, are given by the method
      // reference expression.
      AbstractType receiver = args.remove(0);
      args.add(0, receiver.capture(context));
    }

    for (int i = 0; i < formals.size(); i++) {
      AbstractType ei = args.get(i);
      AbstractType fi = formals.get(i);
      c.add(new Typing(ei, fi, Kind.TYPE_COMPATIBILITY));
    }

    BoundSet newBounds = c.reduce(context);
    assert !newBounds.containsFalse();
    b1.incorporateToFixedPoint(newBounds);

    return b1;
  }

  /**
   * Creates constraints against the target type of {@code invocation} and then reduces and
   * incorporates those constraints with {@code b2}. (See <a
   * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2.1">JLS
   * 18.5.2.1</a>.)
   *
   * @param b2 BoundSet created by {@link #createB2(InvocationType, List, Theta)}
   * @param invocation a method or constructor invocation
   * @param methodType the type of the method or constructor invoked by expression
   * @param target target type of the invocation
   * @param map map of type variables to (inference) variables
   * @return bound set created by constraints against the target type of the invocation
   */
  public BoundSet createB3(
      BoundSet b2,
      ExpressionTree invocation,
      InvocationType methodType,
      AbstractType target,
      Theta map) {
    AbstractType r = methodType.getReturnType(map);
    if (b2.isUncheckedConversion()) {
      // If unchecked conversion was necessary for the method to be applicable during
      // constraint set reduction in 18.5.1, the constraint formula <|R| -> T> is reduced and
      // incorporated with B2.
      BoundSet b =
          new ConstraintSet(new Typing(r.getErased(), target, Kind.TYPE_COMPATIBILITY))
              .reduce(context);
      b2.incorporateToFixedPoint(b);
      return b2;

    } else if (r.isWildcardParameterizedType()) {
      // Otherwise, if r is a parameterized type, G<A1, ..., An>, and one of A1, ...,
      // An is a wildcard, then, for fresh inference variables B1, ..., Bn, the constraint
      // formula <G<B1, ..., Bn> -> T> is reduced and incorporated, along with the bound
      // G<B1, ..., Bn> = capture(G<A1, ..., An>), with B2.
      BoundSet b =
          CaptureBound.createAndIncorporateCaptureConstraint(r, target, invocation, context);
      b2.incorporateToFixedPoint(b);
      return b2;
    } else if (r.isUseOfVariable()) {
      Variable alpha = ((UseOfVariable) r).getVariable();
      // Should a type compatibility constraint be added?
      boolean compatibility = false;
      // If the target type is a reference type, but is not a wildcard-parameterized type.
      if (!target.isWildcardParameterizedType()) {
        // i) B2 contains a bound of one of the forms alpha = S or S <: alpha, where S is a
        // wildcard-parameterized type, or
        compatibility = alpha.getBounds().hasWildcardParameterizedLowerOrEqualBound();
        // ii) B2 contains two bounds of the forms S1 <: alpha and S2 <: alpha, where S1
        // and S2 have supertypes that are two different parameterizations of the same
        // generic class or interface.
        compatibility |= alpha.getBounds().hasLowerBoundDifferentParam();
      } else if (target.isParameterizedType()) {
        // The target type is a parameterization of a generic class or interface, G, and B2
        // contains a
        // bound of one of the forms alpha = S or S <: alpha, where there exists no type of
        // the form G<...> that is a supertype of S, but the raw type |G<...>| is a
        // supertype of S.
        compatibility = alpha.getBounds().hasRawTypeLowerOrEqualBound(target);
      } else if (target.getTypeKind().isPrimitive()) {
        // The target is a primitive type, and one of the primitive wrapper classes
        // mentioned in
        // 5.1.7 is an instantiation, upper bound, or lower bound for alpha in B2.
        compatibility = alpha.getBounds().hasPrimitiveWrapperBound();
      }
      if (compatibility) {
        BoundSet resolve = Resolution.resolve(alpha, b2, context);
        ProperType u = (ProperType) alpha.getBounds().getInstantiation().capture(context);
        ConstraintSet constraintSet =
            new ConstraintSet(new Typing(u, target, Kind.TYPE_COMPATIBILITY));
        BoundSet newBounds = constraintSet.reduce(context);
        resolve.incorporateToFixedPoint(newBounds);
        return resolve;
      }
      if (target.isProper() && target.getJavaType().getKind().isPrimitive()) {
        // From the JLS:
        // "T is a primitive type, and one of the primitive wrapper classes mentioned in
        // 5.1.7 is an instantiation, upper bound, or lower bound for [the variable] in B2."
        ConstraintSet constraintSet = new ConstraintSet(new Typing(r, target, Kind.SUBTYPE));
        BoundSet newBounds = constraintSet.reduce(context);
        b2.incorporateToFixedPoint(newBounds);
        return b2;
      }
    }

    ConstraintSet constraintSet = new ConstraintSet(new Typing(r, target, Kind.TYPE_COMPATIBILITY));
    BoundSet newBounds = constraintSet.reduce(context);
    b2.incorporateToFixedPoint(newBounds);
    return b2;
  }

  /**
   * Creates the constraints between the formal parameters and arguments that are not pertinent to
   * applicability. (See <a
   * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2.2">JLS
   * 18.5.2.2</a>.)
   *
   * @param methodType type of method invoked
   * @param args argument expression trees
   * @param map map from type variable to inference variable
   * @return the constraints between the formal parameters and arguments that are not pertinent to
   *     applicability
   */
  public ConstraintSet createC(
      InvocationType methodType, List<? extends ExpressionTree> args, Theta map) {
    ConstraintSet c = new ConstraintSet();
    List<AbstractType> formals = methodType.getParameterTypes(map, args.size());

    for (int i = 0; i < formals.size(); i++) {
      ExpressionTree ei = args.get(i);
      AbstractType fi = formals.get(i);
      if (notPertinentToApplicability(ei, fi)) {
        c.add(new Expression(ei, fi));
      }
      if (ei.getKind() == Tree.Kind.METHOD_INVOCATION || ei.getKind() == Tree.Kind.NEW_CLASS) {
        if (TreeUtils.isPolyExpression(ei)) {
          AdditionalArgument aa = new AdditionalArgument(ei);
          c.addAll(aa.reduce(context));
        }
      } else {
        // Wait to reduce additional argument constraints from lambdas and method references
        // because the additional constraints might require other inference variables to be
        // resolved before the constraint can be created.
        c.addAll(createAdditionalArgConstraints(ei, fi, map));
      }
    }

    return c;
  }

  /**
   * Adds argument constraints for the argument {@code ei} and its subexpressions. These are in
   * addition to the constraints added in {@link #createC(InvocationType, List, Theta)}.
   *
   * <p>It does this by traversing {@code ei} if it is a method reference, lambda, method
   * invocation, new class tree, conditional expression, switch expression, or parenthesized
   * expression.
   *
   * <p>If {@code ei} is a method invocation or new class tree, that expression might require type
   * argument inference. In that case the additional variables, bounds, and constraints are added
   * here.
   *
   * <p>(See <a
   * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2.2">JLS
   * 18.5.2.2</a>)
   *
   * @param ei expression that is an argument to a method that corresponds to the formal parameter
   *     whose type is {@code fi}
   * @param fi type that is the formal parameter to a method whose corresponding argument is {@code
   *     ei}
   * @param map map from type variable to inference variable
   * @return the additional argument constraints
   */
  private ConstraintSet createAdditionalArgConstraints(
      ExpressionTree ei, AbstractType fi, Theta map) {
    ConstraintSet c = new ConstraintSet();

    switch (ei.getKind()) {
      case MEMBER_REFERENCE:
        c.add(new CheckedExceptionConstraint(ei, fi, map));
        break;
      case LAMBDA_EXPRESSION:
        c.add(new CheckedExceptionConstraint(ei, fi, map));
        LambdaExpressionTree lambda = (LambdaExpressionTree) ei;
        for (ExpressionTree expression : TreeUtils.getReturnedExpressions(lambda)) {
          c.addAll(createAdditionalArgConstraintsNoLambda(expression));
        }
        break;
      case METHOD_INVOCATION:
      case NEW_CLASS:
        if (TreeUtils.isPolyExpression(ei)) {
          c.add(new AdditionalArgument(ei));
        }
        break;
      case PARENTHESIZED:
        c.addAll(createAdditionalArgConstraints(TreeUtils.withoutParens(ei), fi, map));
        break;
      case CONDITIONAL_EXPRESSION:
        ConditionalExpressionTree conditional = (ConditionalExpressionTree) ei;
        c.addAll(createAdditionalArgConstraints(conditional.getTrueExpression(), fi, map));
        c.addAll(createAdditionalArgConstraints(conditional.getFalseExpression(), fi, map));
        break;
      default:
        if (TreeUtils.isSwitchExpression(ei)) {
          SwitchExpressionScanner<Void, Void> scanner =
              new FunctionalSwitchExpressionScanner<>(
                  (ExpressionTree tree, Void unused) -> {
                    c.addAll(createAdditionalArgConstraints(tree, fi, map));
                    return null;
                  },
                  (c1, c2) -> null);
          scanner.scanSwitchExpression(ei, null);
        }
        // no constraints
    }

    return c;
  }

  /**
   * Recursively search for method invocations and new class trees. If any are found, the additional
   * variables, bounds, and constraints are returned. This method is called by {@link
   * #createAdditionalArgConstraints(ExpressionTree, AbstractType, Theta)} when that method
   * encounters a lambda. This method is different because it does not add checked exception
   * constraints for lambdas or method references.
   *
   * @param expression expression to search
   * @return additional constraints
   */
  private ConstraintSet createAdditionalArgConstraintsNoLambda(ExpressionTree expression) {
    ConstraintSet c = new ConstraintSet();

    switch (expression.getKind()) {
      case LAMBDA_EXPRESSION:
        LambdaExpressionTree lambda = (LambdaExpressionTree) expression;
        for (ExpressionTree returnedExpression : TreeUtils.getReturnedExpressions(lambda)) {
          c.addAll(createAdditionalArgConstraintsNoLambda(returnedExpression));
        }
        break;
      case METHOD_INVOCATION:
      case NEW_CLASS:
        if (TreeUtils.isPolyExpression(expression)) {
          c.add(new AdditionalArgument(expression));
        }
        break;
      case PARENTHESIZED:
        c.addAll(createAdditionalArgConstraintsNoLambda(TreeUtils.withoutParens(expression)));
        break;
      case CONDITIONAL_EXPRESSION:
        ConditionalExpressionTree conditional = (ConditionalExpressionTree) expression;
        c.addAll(createAdditionalArgConstraintsNoLambda(conditional.getTrueExpression()));
        c.addAll(createAdditionalArgConstraintsNoLambda(conditional.getFalseExpression()));
        break;
      default:
        if (TreeUtils.isSwitchExpression(expression)) {
          SwitchExpressionScanner<Void, Void> scanner =
              new FunctionalSwitchExpressionScanner<>(
                  (ExpressionTree tree, Void unused) -> {
                    c.addAll(createAdditionalArgConstraintsNoLambda(tree));
                    return null;
                  },
                  (c1, c2) -> null);
          scanner.scanSwitchExpression(expression, null);
        }
        // no constraints
    }

    return c;
  }

  /**
   * <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.12.2.2">JLS
   * 15.12.2.2</a> (Assuming the method is a generic method and the method invocation does not
   * provide explicit type arguments)
   *
   * @param expressionTree expression tree
   * @param formalParameterType the formal parameter type of the method invocation
   * @return whether {@code expressionTree} is pertinent to applicability
   */
  private boolean notPertinentToApplicability(
      ExpressionTree expressionTree, AbstractType formalParameterType) {
    switch (expressionTree.getKind()) {
      case LAMBDA_EXPRESSION:
        LambdaExpressionTree lambda = (LambdaExpressionTree) expressionTree;
        if (TreeUtils.isImplicitlyTypedLambda(lambda) || formalParameterType.isUseOfVariable()) {
          // An implicitly typed lambda expression.
          return true;
        } else {
          // An explicitly typed lambda expression whose body is a block,
          // where at least one result expression is not pertinent to applicability.
          // An explicitly typed lambda expression whose body is an expression that is
          // not pertinent to applicability.
          AbstractType funcReturn = formalParameterType.getFunctionTypeReturnType();
          for (ExpressionTree result : TreeUtils.getReturnedExpressions(lambda)) {
            if (notPertinentToApplicability(result, funcReturn)) {
              return true;
            }
          }
          return false;
        }
      case MEMBER_REFERENCE:
        // An inexact method reference expression.
        return formalParameterType.isUseOfVariable()
            || !TreeUtils.isExactMethodReference((MemberReferenceTree) expressionTree);
      case PARENTHESIZED:
        // A parenthesized expression whose contained expression is not pertinent to
        // applicability.
        return notPertinentToApplicability(
            TreeUtils.withoutParens(expressionTree), formalParameterType);
      case CONDITIONAL_EXPRESSION:
        ConditionalExpressionTree conditional = (ConditionalExpressionTree) expressionTree;
        // A conditional expression whose second or third operand is not pertinent to
        // applicability.
        return notPertinentToApplicability(conditional.getTrueExpression(), formalParameterType)
            || notPertinentToApplicability(conditional.getFalseExpression(), formalParameterType);
      default:
        if (TreeUtils.isSwitchExpression(expressionTree)) {
          SwitchExpressionScanner<Boolean, Void> scanner =
              new FunctionalSwitchExpressionScanner<>(
                  (ExpressionTree tree, Void unused) ->
                      notPertinentToApplicability(tree, formalParameterType),
                  (r1, r2) -> (r1 != null && r1) || (r2 != null && r2));
          ;
          return scanner.scanSwitchExpression(expressionTree, null);
        }
        return false;
    }
  }

  /**
   * Returns the result of reducing and incorporating the set of constraints, {@code c}. The
   * constraints must be reduced in a particular order. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2.2">JLS
   * 18.5.2.2</a>.
   *
   * @param b3 bound set created by previous inference step that is sideeffect and returned
   * @param c constraints that are reduced and incorporated
   * @return the result of reducing and incorporating the set of constraints
   */
  private BoundSet getB4(BoundSet b3, ConstraintSet c) {
    // C might contain new variables that have not yet been added to the b3 bound set.
    Set<Variable> newVariables = c.getAllInferenceVariables();
    while (!c.isEmpty()) {

      ConstraintSet subset = c.getClosedSubset(b3.getDependencies(newVariables));
      Set<Variable> alphas = subset.getAllInputVariables();
      if (!alphas.isEmpty()) {
        // First resolve only the variables with proper bounds.
        for (Variable alpha : new ArrayList<>(alphas)) {
          if (alpha.getBounds().onlyProperBounds()) {
            Resolution.resolve(alpha, b3, context);
            alphas.remove(alpha);
          }
        }
        c.applyInstantiations();
      }
      if (!alphas.isEmpty()) {
        // Resolve any remaining variables that have bounds that are variable or inference
        // types.
        Resolution.resolve(alphas, b3, context);
        c.applyInstantiations();
      }
      c.remove(subset);
      BoundSet newBounds = subset.reduceAdditionalArgOnce(context);
      if (!subset.isEmpty()) {
        // The subset is not empty at this point if an additional argument constraint was
        // found.  In this case, a new subset needs to be picked so that dependencies of
        // the constraints from reducing the additional argument constraint can be taken
        // into account.
        c.addAll(subset);
      }
      b3.incorporateToFixedPoint(newBounds);
    }
    return b3;
  }
}
