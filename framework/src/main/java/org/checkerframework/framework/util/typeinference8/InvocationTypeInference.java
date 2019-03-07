package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.WildcardType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;
import org.checkerframework.framework.util.typeinference8.bound.CaptureBound;
import org.checkerframework.framework.util.typeinference8.constraint.CheckedExceptionConstraint;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint.Kind;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.Expression;
import org.checkerframework.framework.util.typeinference8.constraint.Typing;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.InferenceFactory;
import org.checkerframework.framework.util.typeinference8.types.InvocationType;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.FalseBoundException;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Resolution;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Performs invocation type inference as described in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2">JLS Section
 * 18.5.2</a>. Main entry point is {@link InvocationTypeInference#infer(ExpressionTree,
 * AnnotatedExecutableType)}
 *
 * <p>Invocation type inference is the process by which method type arguments are inferred for a
 * given method invocation. An overview of the process is given below.
 *
 * <p>1. Inference creates a variable for each method type argument for a given method invocation.
 * Each variable may zero or more upper, lower, and equal bounds. The bounds of a variable are
 * initially the bounds on the type argument. More bounds may be infered in later steps.
 *
 * <p>Bounds are between a variable and an abstract type. {@link AbstractType}s are type-like
 * structures that might include variables. Abstract types might also be a variable or a type
 * without any inverence variables, which is also know as a proper type.
 *
 * <p>Variables are represented by {@link Variable} objects which holds bounds for the variable in
 * an {@link org.checkerframework.framework.util.typeinference8.types.VariableBounds} object.
 * Additional variables may be created in later steps if any subexpression of the method invocation
 * requires type inference.
 *
 * <p>2. Next, inference creates constraints between the arguments to the method invocation and its
 * formal parameters. Also, for non-void methods, a constraint between the declared return type and
 * the "target type" of the method invocation is created. "Target types" are defined in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html">JLS Chapter 5</a>. For
 * example, the target type of a method invocation assigned to a variable is the type of the
 * variable.
 *
 * <p>Constraints are represented by {@link Constraint} objectst and are between abstract types, see
 * {@link AbstractType} and either expressions, see {@link Expression}, or other abstract types. A
 * constraint might also be an abstract type that might be thrown by the method invocation, see
 * {@link CheckedExceptionConstraint}. Groups of constraints are stored in {@link ConstraintSet}s.
 *
 * <p>3. Next, these constraints are "reduced" producing bounds on the variables. Reduction depends
 * on the kind of constraint and is defined in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.2">JLS section
 * 18.2</a>. . In this code base, constraints are reduced via {@link
 * ConstraintSet#reduce(Java8InferenceContext)}.
 *
 * <p>4. The variables' bounds are then "incorporated" which produces more bounds and/or constraints
 * that must then be "reduced" or "incorporated". Incorporation and reduction continue until no new
 * bounds or constraints are produced. Bounds are incorporated via {@link
 * BoundSet#incorporateToFixedPoint(BoundSet)}. Incorporation in defined in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.3">JLS section
 * 18.3</a>.
 *
 * <p>5. Finally, a type for each variable is computed by "resolving" the bounds.
 *
 * <p>Variables are resolved via {@link Resolution#resolve(BoundSet, Queue)}. Resolution is defined
 * in the <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.4">JLS
 * section 18.4</a>.
 */
public class InvocationTypeInference {

    protected final SourceChecker checker;
    protected Java8InferenceContext context;

    public InvocationTypeInference(AnnotatedTypeFactory factory, TreePath pathToExpression) {
        this.checker = factory.getContext().getChecker();
        this.context = new Java8InferenceContext(factory, pathToExpression, this);
    }

    /**
     * Returns a mapping of type variable to type argument computed using the type of {@code
     * methodInvocationTree} and the return type of {@code methodType}.
     */
    public static Map<TypeVariable, TypeMirror> getMappingFromReturnType(
            ExpressionTree methodInvocationTree,
            ExecutableType methodType,
            ProcessingEnvironment env) {
        TypeMirror methodCallType = TreeUtils.typeOf(methodInvocationTree);
        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) env;
        Types types = Types.instance(javacEnv.getContext());
        GetMapping mapping = new GetMapping(methodType.getTypeVariables(), types);
        mapping.visit(methodType.getReturnType(), methodCallType);
        return mapping.subs;
    }

    public Java8InferenceContext getContext() {
        return context;
    }

    /**
     * Perform invocation type inference on {@code invocation}. See <a
     * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2">JLS
     * 18.5.2</a>
     */
    public List<Variable> infer(ExpressionTree invocation, AnnotatedExecutableType methodType) {
        Tree assignmentContext = TreeUtils.getAssignmentContext(context.pathToExpression);
        if (!shouldTryInference(assignmentContext, context.pathToExpression)) {
            return null;
        }
        ExecutableType e = InferenceFactory.getTypeOfMethodAdaptedToUse(invocation, context);
        List<Variable> result;
        try {
            InvocationType invocationType = new InvocationType(methodType, e, invocation, context);
            result = inferInternal(invocation, invocationType);
        } catch (FalseBoundException ex) {
            if (ex.isAnnotatedTypeFailed()) {
                checker.report(Result.failure("type.inference.failed"), invocation);
            } else {
                // Catch any exception so all crashes in a compilation unit are reported.
                logException(invocation, ex);
            }
            return null;
        } catch (Exception ex) {
            // Catch any exception so all crashes in a compilation unit are reported.
            logException(invocation, ex);
            return null;
        }

        checkResult(result, invocation, e);
        return result;
    }

    /**
     * Perform invocation type inference on {@code invocation}. See <a
     * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2">JLS
     * 18.5.2</a>
     */
    private List<Variable> inferInternal(ExpressionTree invocation, InvocationType invocationType) {
        ProperType target = context.inferenceTypeFactory.getTargetType();
        List<? extends ExpressionTree> args;
        if (invocation.getKind() == Tree.Kind.METHOD_INVOCATION) {
            args = ((MethodInvocationTree) invocation).getArguments();
        } else {
            args = ((NewClassTree) invocation).getArguments();
        }

        Theta map = context.inferenceTypeFactory.createTheta(invocation, invocationType, context);
        BoundSet b2 = createB2(invocationType, args, map);
        BoundSet b3;
        if (target != null && TreeUtils.isPolyExpression(invocation)) {
            b3 = createB3(b2, invocation, invocationType, target, map);
        } else {
            b3 = b2;
        }
        ConstraintSet c = createC(invocationType, args, map);

        BoundSet b4 = getB4(b3, c);
        List<Variable> thetaPrime = b4.resolve();

        if (b4.isUncheckedConversion()) {
            // If unchecked conversion was necessary for the method to be applicable during
            // constraint set reduction in 18.5.1, then the parameter types of the invocation type
            // of m are obtained by applying thetaPrime to the parameter types of m's type, and the
            // return
            // type and thrown types of the invocation type of m are given by the erasure of the
            // return type and thrown types of m's type.
            // TODO: the erasure of the return type should happen were the inferred type arguments
            // are substituted into the method type.
        }
        return thetaPrime;
    }

    /**
     * Creates the bound set used to determine whether a method is applicable. This method is called
     * B2 in <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.1">JLS
     * Section 18.5.1</a>
     *
     * <p>It does this by:
     *
     * <ol>
     *   <li value="1">Creating the variables to be solved and initializing their bounds based on
     *       the type parameter declaration.
     *   <li value="2">Adding any bounds implied by the throws clause of {@code methodType}.
     *   <li value="3">Constructing constraints between formal parameters and arguments that are
     *       pertinent to applicable. Generally, all arguments are applicable except: inexact method
     *       reference, implicitly typed lambdas, or explicitly typed lambda whose return
     *       expression(s) are not pertinent. See <a
     *       href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.12.2.2">JLS
     *       Section 15.12.2.2</a> for more details. *
     *   <li value="4">Reducing and incorporating those constraints which finally produces B2
     * </ol>
     *
     * @param methodType the type of the method or constructor invoked
     * @param args argument expression tress
     * @param map map of type variables to (inference) variables
     * @return bound set used to determine whether a method is applicable.
     */
    public BoundSet createB2(
            InvocationType methodType, List<? extends ExpressionTree> args, Theta map) {
        BoundSet b0 = BoundSet.initialBounds(map, context);

        // For all i (1 <= i <= p), if Pi appears in the throws clause of m, then the bound throws
        // alphai is implied. These bounds, if any, are incorporated with B0 to produce a new bound
        // set, B1.
        for (AbstractType thrownType : methodType.getThrownTypes(map)) {
            if (thrownType.isVariable()) {
                ((Variable) thrownType).getBounds().setHasThrowsBound(true);
            }
        }

        BoundSet b1 = b0;
        ConstraintSet c = new ConstraintSet();
        List<AbstractType> formals = methodType.getParameterTypes(map, args.size());

        for (int i = 0; i < formals.size(); i++) {
            ExpressionTree ei = args.get(i);
            AbstractType fi = formals.get(i);

            if (!notPertinentToApplicability(ei, fi.isVariable())) {
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
     */
    public BoundSet createB2MethodRef(
            InvocationType methodType, List<AbstractType> args, Theta map) {
        BoundSet b0 = BoundSet.initialBounds(map, context);

        // For all i (1 <= i <= p), if Pi appears in the throws clause of m, then the bound throws
        // alphai is implied. These bounds, if any, are incorporated with B0 to produce a new bound
        // set, B1.
        for (AbstractType thrownType : methodType.getThrownTypes(map)) {
            if (thrownType.isVariable()) {
                ((Variable) thrownType).getBounds().setHasThrowsBound(true);
            }
        }

        BoundSet b1 = b0;
        ConstraintSet c = new ConstraintSet();
        List<AbstractType> formals = methodType.getParameterTypes(map, args.size());

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
     * Creates constraints against the targets type of {@code invocation} and then reduces and
     * incorporates those constraints with {@code b2}. (See <a
     * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2.1">JLS
     * 18.5.2.1</a>.)
     *
     * @param b2 BoundSet created by {@link #createB2(InvocationType, List, Theta)}
     * @param invocation method or constructor invocation
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
            // Otherwise, if R theta is a parameterized type, G<A1, ..., An>, and one of A1, ...,
            // An is a wildcard, then, for fresh inference variables B1, ..., Bn, the constraint
            // formula <G<B1, ..., Bn> -> T> is reduced and incorporated, along with the bound
            // G<B1, ..., Bn> = capture(G<A1, ..., An>), with B2.
            CaptureBound capture = new CaptureBound(r, invocation, context);
            BoundSet b = capture.incorporate(target, context);
            b2.incorporateToFixedPoint(b);
            return b2;
        } else if (r.isVariable()) {
            Variable alpha = (Variable) r;
            boolean compatiblity = false;
            // T is a reference type, but is not a wildcard-parameterized type, and either
            if (!target.isWildcardParameterizedType()) {
                // i) B2 contains a bound of one of the forms alpha = S or S <: alpha, where S is a
                // wildcard-parameterized type, or
                compatiblity = alpha.getBounds().hasWildcardParameterizedLowerOrEqualBound();
                if (!compatiblity) {
                    // ii) B2 contains two bounds of the forms S1 <: alpha and S2 <: alpha, where S1
                    // and S2
                    // have supertypes that are two different parameterizations of the same generic
                    // class or interface.
                    compatiblity = alpha.getBounds().hasLowerBoundDifferentParam();
                }
            }
            if (target.isParameterizedType()) {
                // T is a parameterization of a generic class or interface, G, and B2 contains a
                // bound of one of the forms alpha = S or S <: alpha, where there exists no type of
                // the form
                // G<...> that is a supertype of S, but the raw type |G<...>| is a supertype of S.
                compatiblity = alpha.getBounds().hasRawTypeLowerOrEqualBound(target);
            }
            if (target.getTypeKind().isPrimitive()) {
                // T is a primitive type, and one of the primitive wrapper classes mentioned in
                // 5.1.7
                // is an instantiation, upper bound, or lower bound for alpha in B2.
                compatiblity = alpha.getBounds().hasPrimitiveWrapperBound();
            }
            if (compatiblity) {
                BoundSet resolve = Resolution.resolve(alpha, b2, context);
                ProperType u = (ProperType) alpha.getBounds().getInstantiation().capture();
                ConstraintSet constraintSet =
                        new ConstraintSet(new Typing(u, target, Kind.TYPE_COMPATIBILITY));
                BoundSet newBounds = constraintSet.reduce(context);
                resolve.incorporateToFixedPoint(newBounds);
                return resolve;
            }
        }
        ConstraintSet constraintSet =
                new ConstraintSet(new Typing(r, target, Kind.TYPE_COMPATIBILITY));
        BoundSet newBounds = constraintSet.reduce(context);
        b2.incorporateToFixedPoint(newBounds);
        return b2;
    }

    /**
     * Creates the constraints between the formal parameters and arguments that are not pertinent to
     * applicability. (See <a
     * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2.2">JLS
     * 18.5.2.2</a>)
     *
     * @param methodType type of method invoked
     * @param args argument expression trees
     * @param map map from type variable to inference variable
     * @return the constraints between the formal parameters and arguments that are not pertinent to
     *     applicability
     */
    private ConstraintSet createC(
            InvocationType methodType, List<? extends ExpressionTree> args, Theta map) {
        ConstraintSet c = new ConstraintSet();
        List<AbstractType> formals = methodType.getParameterTypes(map, args.size());

        for (int i = 0; i < formals.size(); i++) {
            ExpressionTree ei = args.get(i);
            AbstractType fi = formals.get(i);
            if (notPertinentToApplicability(ei, fi.isVariable())) {
                c.add(new Expression(ei, fi));
            }
            c.addAll(createAdditionalArgConstraints(ei, fi, map));
        }

        return c;
    }

    /**
     * Adds additional argument constraints for the argument {@code ei} and its subexpressions.
     *
     * <p>It does this by traversing {@code ei} if it is a method reference, lambda, method
     * invocation, new class tree, conditional expression, or parenthesized expression.
     *
     * <p>If {@code ei} is a method invocation or new class tree, that expression might require type
     * argument inference. In that case then additional variables, bounds, and constraints are added
     * here.
     *
     * <p>(See <a
     * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2.2">JLS
     * 18.5.2.2</a>)
     *
     * @param ei expression that is an argument to a method that corresponds to the formal
     *     parameter, {@code fi}
     * @param fi type that is the formal parameter to a method whose corresponding argument is
     *     {@code ei}
     * @param map map from type variable to inference variable
     * @return the additional argument constraints.
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
                    c.addAll(createAdditionalArgConstraints(expression, fi, map));
                }
                break;
            case METHOD_INVOCATION:
                if (TreeUtils.isPolyExpression(ei)) {
                    MethodInvocationTree methodInvocation = (MethodInvocationTree) ei;
                    InvocationType methodType =
                            context.inferenceTypeFactory.getTypeOfMethodAdaptedToUse(
                                    methodInvocation);
                    Theta newMap =
                            context.inferenceTypeFactory.createTheta(
                                    methodInvocation, methodType, context);
                    c.addAll(createC(methodType, methodInvocation.getArguments(), newMap));
                }
                break;
            case NEW_CLASS:
                if (TreeUtils.isPolyExpression(ei)) {
                    NewClassTree newClassTree = (NewClassTree) ei;
                    InvocationType methodType =
                            context.inferenceTypeFactory.getTypeOfMethodAdaptedToUse(newClassTree);

                    Theta newMap =
                            context.inferenceTypeFactory.createTheta(
                                    newClassTree, methodType, context);
                    c.addAll(createC(methodType, newClassTree.getArguments(), newMap));
                }
                break;
            case PARENTHESIZED:
                c.addAll(createAdditionalArgConstraints(TreeUtils.skipParens(ei), fi, map));
                break;
            case CONDITIONAL_EXPRESSION:
                ConditionalExpressionTree conditional = (ConditionalExpressionTree) ei;
                c.addAll(createAdditionalArgConstraints(conditional.getTrueExpression(), fi, map));
                c.addAll(createAdditionalArgConstraints(conditional.getFalseExpression(), fi, map));
                break;
            default:
                // no constraints
        }

        return c;
    }

    /**
     * <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-15.12.2.2">JLS
     * 15.12.2.2</a> (Assuming the method is a generic method and the method invocation does not
     * provide explicit type arguments)
     *
     * @param expressionTree expression tree
     * @param isTargetVariable whether the corresponding target type (as derived from the signature
     *     of m) is a type parameter of m
     * @return whether or not {@code expressionTree} is pertinent to applicability
     */
    private boolean notPertinentToApplicability(
            ExpressionTree expressionTree, boolean isTargetVariable) {
        switch (expressionTree.getKind()) {
            case LAMBDA_EXPRESSION:
                LambdaExpressionTree lambda = (LambdaExpressionTree) expressionTree;
                if (TreeUtils.isImplicitlyTypedLambda(lambda) || isTargetVariable) {
                    // An implicitly typed lambda expression.
                    return true;
                } else {
                    // An explicitly typed lambda expression whose body is a block,
                    // where at least one result expression is not pertinent to applicability.
                    // An explicitly typed lambda expression whose body is an expression that is
                    // not pertinent to applicability.
                    for (ExpressionTree result : TreeUtils.getReturnedExpressions(lambda)) {
                        if (notPertinentToApplicability(result, isTargetVariable)) {
                            return true;
                        }
                    }
                    return false;
                }
            case MEMBER_REFERENCE:
                // An inexact method reference expression.
                return isTargetVariable
                        || !TreeUtils.isExactMethodReference((MemberReferenceTree) expressionTree);
            case PARENTHESIZED:
                // A parenthesized expression whose contained expression is not pertinent to
                // applicability.
                return notPertinentToApplicability(
                        TreeUtils.skipParens(expressionTree), isTargetVariable);
            case CONDITIONAL_EXPRESSION:
                ConditionalExpressionTree conditional = (ConditionalExpressionTree) expressionTree;
                // A conditional expression whose second or third operand is not pertinent to
                // applicability.
                return notPertinentToApplicability(
                                conditional.getTrueExpression(), isTargetVariable)
                        || notPertinentToApplicability(
                                conditional.getFalseExpression(), isTargetVariable);
            default:
                return false;
        }
    }

    /**
     * Returns the result of reducing and incorporating the set of constraints, {@code c}. The
     * constraints must be reduced in a particular order. See <a
     * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2.2">JLS
     * 18.5.2.2</a>.
     */
    private BoundSet getB4(BoundSet current, ConstraintSet c) {
        // C might contain new variables that have not yet been added to the current bound set.
        Set<Variable> newVariables = c.getAllInferenceVariables();
        while (!c.isEmpty()) {
            ConstraintSet subset = c.getClosedSubset(current.getDependencies(newVariables));
            Set<Variable> alphas = subset.getAllInputVariables();
            if (!alphas.isEmpty()) {
                BoundSet resolved = Resolution.resolve(alphas, current, context);
                c.applyInstantiations(resolved.getInstantiationsInAlphas(alphas));
            }
            c.remove(subset);
            BoundSet newBounds = subset.reduce(context);
            current.incorporateToFixedPoint(newBounds);
        }
        return current;
    }

    /** Convert the exceptions into a checker error and report it. */
    private void logException(ExpressionTree methodInvocation, Exception ex) {
        if (ex instanceof ProperType.CantCompute) {
            return;
        }
        StringBuilder message = new StringBuilder();
        message.append(ex.getLocalizedMessage());
        if (checker.hasOption("printErrorStack")) {
            message.append("\n").append(formatStackTrace(ex.getStackTrace()));
        }
        checker.report(
                Result.failure("type.inference.crash", message.toString()), methodInvocation);
    }

    /** Format a list of {@link StackTraceElement}s to be printed out as an error message. */
    protected String formatStackTrace(StackTraceElement[] stackTrace) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        if (stackTrace.length == 0) {
            sb.append("no stack trace available.");
        } else {
            sb.append("Stack trace: ");
        }
        for (StackTraceElement ste : stackTrace) {
            if (!first) {
                sb.append("\n");
            }
            first = false;
            sb.append(ste.toString());
        }
        return sb.toString();
    }

    /**
     * Is the leaf of the path a generic method invocation that elides method type arguments that
     * does not require a invocation type inference to determine its target type?
     *
     * @param assignmentContext tree to which the leaf of path is assigned
     * @param path path to the method invocation
     * @return if inference should be preformed.
     */
    private boolean shouldTryInference(Tree assignmentContext, TreePath path) {
        if (path.getParentPath().getLeaf().getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
            return false;
        }
        if (path.getLeaf().getKind() == Tree.Kind.METHOD_INVOCATION) {
            MethodInvocationTree tree = (MethodInvocationTree) path.getLeaf();
            ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
            if (TreeUtils.isPolyExpression(receiver)) {
                return false;
            }
        }

        if (assignmentContext == null) {
            return true;
        }
        switch (assignmentContext.getKind()) {
            case RETURN:
                HashSet<Tree.Kind> kinds =
                        new HashSet<>(Arrays.asList(Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.METHOD));
                Tree enclosing = TreeUtils.enclosingOfKind(path, kinds);
                return enclosing.getKind() != Tree.Kind.LAMBDA_EXPRESSION;
            case METHOD_INVOCATION:
                MethodInvocationTree methodInvocationTree =
                        (MethodInvocationTree) assignmentContext;
                if (methodInvocationTree.getTypeArguments().isEmpty()) {
                    ExecutableElement ele = TreeUtils.elementFromUse(methodInvocationTree);
                    return ele.getTypeParameters().isEmpty();
                }
                return false;
            default:
                return !(assignmentContext instanceof ExpressionTree
                        && TreeUtils.isPolyExpression((ExpressionTree) assignmentContext));
        }
    }

    /**
     * Issues an error if the type arguments computed by this class do not match those computed by
     * javac.
     */
    private void checkResult(
            List<Variable> result, ExpressionTree invocation, ExecutableType methodType) {
        Map<TypeVariable, TypeMirror> fromReturn =
                getMappingFromReturnType(invocation, methodType, context.env);
        for (Variable variable : result) {
            if (!variable.getInvocation().equals(invocation)) {
                continue;
            }
            TypeVariable typeVariable = variable.getJavaType();
            if (fromReturn.containsKey(typeVariable)) {
                TypeMirror correctType = fromReturn.get(typeVariable);
                TypeMirror inferredType = variable.getBounds().getInstantiation().getJavaType();
                if (context.types.isSameType(
                        context.types.erasure((Type) correctType),
                        context.types.erasure((Type) inferredType),
                        false)) {
                    if (areSameCapture(correctType, inferredType)) {
                        continue;
                    }
                }
                if (!context.types.isSameType((Type) correctType, (Type) inferredType, false)) {
                    // type.inference.not.same=type variable: %s\ninferred: %s\njava type: %s
                    checker.report(
                            Result.failure(
                                    "type.inference.not.same",
                                    typeVariable + "(" + variable + ")",
                                    inferredType,
                                    correctType),
                            invocation);
                }
            }
        }
    }

    /** @return true if actual and inferred are captures of the same wildcard or declared type. */
    private boolean areSameCapture(TypeMirror actual, TypeMirror inferred) {
        if (TypesUtils.isCaptured(actual) && TypesUtils.isCaptured(inferred)) {
            return context.types.isSameWildcard(
                    (WildcardType) TypesUtils.getCapturedWildcard((TypeVariable) actual),
                    (Type) TypesUtils.getCapturedWildcard((TypeVariable) inferred));
        } else if (TypesUtils.isCaptured(actual) && inferred.getKind() == TypeKind.WILDCARD) {
            return context.types.isSameWildcard(
                    (WildcardType) TypesUtils.getCapturedWildcard((TypeVariable) actual),
                    (Type) inferred);
        } else if (actual.getKind() == TypeKind.DECLARED
                && inferred.getKind() == TypeKind.DECLARED) {
            DeclaredType actualDT = (DeclaredType) actual;
            DeclaredType inferredDT = (DeclaredType) inferred;
            if (actualDT.getTypeArguments().size() == inferredDT.getTypeArguments().size()) {
                for (int i = 0; i < actualDT.getTypeArguments().size(); i++) {
                    if (!areSameCapture(
                            actualDT.getTypeArguments().get(i),
                            inferredDT.getTypeArguments().get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Helper class for {@link #getMappingFromReturnType(ExpressionTree, ExecutableType,
     * ProcessingEnvironment)}
     */
    private static class GetMapping implements TypeVisitor<Void, TypeMirror> {

        final Map<TypeVariable, TypeMirror> subs = new HashMap<>();
        final List<? extends TypeVariable> typeVariables;
        final Types types;

        public GetMapping(List<? extends TypeVariable> typeVariables, Types types) {
            this.typeVariables = typeVariables;
            this.types = types;
        }

        @Override
        public Void visit(TypeMirror t, TypeMirror mirror) {
            if (t == null || mirror == null) {
                return null;
            }
            return t.accept(this, mirror);
        }

        @Override
        public Void visit(TypeMirror t) {
            return null;
        }

        @Override
        public Void visitPrimitive(PrimitiveType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitNull(NullType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitArray(ArrayType t, TypeMirror mirror) {
            assert mirror.getKind() == TypeKind.ARRAY : mirror;
            return visit(t.getComponentType(), ((ArrayType) mirror).getComponentType());
        }

        @Override
        public Void visitDeclared(DeclaredType t, TypeMirror mirror) {
            assert mirror.getKind() == TypeKind.DECLARED : mirror;
            DeclaredType param = (DeclaredType) mirror;
            if (types.isSubtype((Type) mirror, (Type) param)) {
                param = (DeclaredType) types.asSuper((Type) mirror, ((Type) param).asElement());
            }
            if (t.getTypeArguments().size() == param.getTypeArguments().size()) {
                for (int i = 0; i < t.getTypeArguments().size(); i++) {
                    visit(t.getTypeArguments().get(i), param.getTypeArguments().get(i));
                }
            }
            return null;
        }

        @Override
        public Void visitError(ErrorType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitTypeVariable(TypeVariable t, TypeMirror mirror) {
            if (typeVariables.contains(t)) {
                subs.put(t, mirror);
            } else if (mirror.getKind() == TypeKind.TYPEVAR) {
                TypeVariable param = (TypeVariable) mirror;
                visit(t.getUpperBound(), param.getUpperBound());
                visit(t.getLowerBound(), param.getLowerBound());
            }
            // else it's not a method type variable
            return null;
        }

        @Override
        public Void visitWildcard(javax.lang.model.type.WildcardType t, TypeMirror mirror) {
            if (mirror.getKind() == TypeKind.WILDCARD) {
                javax.lang.model.type.WildcardType param =
                        (javax.lang.model.type.WildcardType) mirror;
                visit(t.getExtendsBound(), param.getExtendsBound());
                visit(t.getSuperBound(), param.getSuperBound());
            } else if (mirror.getKind() == TypeKind.TYPEVAR) {
                TypeVariable param = (TypeVariable) mirror;
                visit(t.getExtendsBound(), param.getUpperBound());
                visit(t.getSuperBound(), param.getLowerBound());
            } else {
                assert false : mirror;
            }
            return null;
        }

        @Override
        public Void visitExecutable(ExecutableType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitNoType(NoType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitUnknown(TypeMirror t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitUnion(UnionType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitIntersection(IntersectionType t, TypeMirror mirror) {
            assert mirror.getKind() == TypeKind.INTERSECTION : mirror;
            IntersectionType param = (IntersectionType) mirror;
            assert t.getBounds().size() == param.getBounds().size();

            for (int i = 0; i < t.getBounds().size(); i++) {
                visit(t.getBounds().get(i), param.getBounds().get(i));
            }

            return null;
        }
    }
}
