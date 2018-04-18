package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.WildcardType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.typeinference8.InvocationTypeInference;
import org.checkerframework.javacutil.typeinference8.Resolution;
import org.checkerframework.javacutil.typeinference8.bound.BoundSet;
import org.checkerframework.javacutil.typeinference8.constraint.CheckedExceptionConstraint;
import org.checkerframework.javacutil.typeinference8.constraint.Constraint;
import org.checkerframework.javacutil.typeinference8.constraint.ConstraintSet;
import org.checkerframework.javacutil.typeinference8.constraint.Expression;
import org.checkerframework.javacutil.typeinference8.constraint.Typing;
import org.checkerframework.javacutil.typeinference8.typemirror.type.VariableTypeMirror;
import org.checkerframework.javacutil.typeinference8.types.AbstractType;
import org.checkerframework.javacutil.typeinference8.types.InvocationType;
import org.checkerframework.javacutil.typeinference8.types.Variable;
import org.checkerframework.javacutil.typeinference8.util.InferenceUtils;
import org.checkerframework.javacutil.typeinference8.util.Java8InferenceContext;

/**
 * Performs invocation type inference as described in JLS Chapter 18.5.2. Main entry point is {@link
 * #infer(ExpressionTree)}.
 *
 * <p>At a high level, inference creates variables, as place holders for the method type arguments
 * to infer for the invocation of a method. Then it creates constraints between the arguments to the
 * method invocation and its formal parameter types and the return type of the method and the target
 * type of the invocation. These constraints are reduced to produce bounds on the variables. These
 * variables are then incorporated, which produces more bounds or constraints. Then a type for each
 * variable is computed by resolving the bounds.
 *
 * <p>{@link AbstractType}s are type-like structures that might include inference variables.
 *
 * <p>Constraints, {@link Constraint}, are between abstract types and either expressions, see {@link
 * Expression}; other abstract types, see {@link Typing}; or abstract types that might be thrown,
 * see {@link CheckedExceptionConstraint}. They are reduced by invoking {@link Constraint#reduce}.
 * Groups of constraints are stored in {@link ConstraintSet}s.
 *
 * <p>Bounds are between an inference variable and another abstract type, including another
 * variable. They are stored in {@link VariableTypeMirror} and {@link VariableTypeMirror}s are
 * stored in {@link BoundSet}s.
 *
 * <p>Variables are resolved via {@link Resolution#resolve(LinkedHashSet, BoundSet)}.
 */
public class CFInvocationTypeInference extends InvocationTypeInference {

    private final SourceChecker checker;

    public CFInvocationTypeInference(AnnotatedTypeFactory factory, TreePath pathToExpression) {
        this.context =
                new Java8InferenceContext(
                        factory.getProcessingEnv(),
                        factory.getContext().getTypeUtils(),
                        pathToExpression,
                        this);
        this.checker = factory.getContext().getChecker();
    }

    @Override
    public List<Variable> infer(ExpressionTree invocation) {
        Tree assignmentContext = TreeUtils.getAssignmentContext(context.pathToExpression);
        if (!shouldTryInference(assignmentContext, context.pathToExpression)) {
            return null;
        }
        List<Variable> result;
        try {
            result = super.infer(invocation);
        } catch (Exception ex) {
            // Catch any exception so all crashes in a compilation unit are reported.
            logException(invocation, ex);
            return null;
        }
        InvocationType methodType =
                context.inferenceTypeFactory.getTypeOfMethodAdaptedToUse(invocation);
        checkResult(result, invocation, methodType);
        return result;
    }

    /** Convert the exceptions into a checker error and report it. */
    private void logException(ExpressionTree methodInvocation, java.lang.Exception ex) {
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
            List<Variable> result, ExpressionTree invocation, InvocationType methodType) {
        Map<TypeVariable, TypeMirror> fromReturn =
                InferenceUtils.getMappingFromReturnType(
                        invocation, methodType.getJavaType(), context.env);
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
}
