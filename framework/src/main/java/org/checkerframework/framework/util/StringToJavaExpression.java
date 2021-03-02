package org.checkerframework.framework.util;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Given an expression string, convert it to a {@link JavaExpression}. See {@link #toJavaExpression
 * )}.
 */
@AnnotatedFor("nullness")
@FunctionalInterface
public interface StringToJavaExpression {

    /**
     * Convert {@code stringExpr} to {@link JavaExpression}.
     *
     * <p>If no conversion exists, {@code null} is returned.
     *
     * <p>Conversion includes parsing {@code stringExpr} to a {@code JavaExpression} and optional
     * transforming the result of parsing into an other {@code JavaExpression}.
     *
     * @param stringExpr an string expression
     * @return a {@code JavaExpression} or {@code null} if no conversion from {@code stringExpr}
     *     exists
     * @throws JavaExpressionParseException if {@code stringExpr} cannot be parsed to a {@code
     *     JavaExpression}
     */
    @Nullable JavaExpression toJavaExpression(String stringExpr) throws JavaExpressionParseException;

    /**
     * Parses {@code expression} to a {@link JavaExpression} as if it were written at {@code
     * classDecl}.
     *
     * @param expression a string expression
     * @param classDecl type element at which {@code expression} is parsed.
     * @param checker checker used to get the {@link
     *     javax.annotation.processing.ProcessingEnvironment} and current {@link
     *     com.sun.source.tree.CompilationUnitTree}
     * @return the {@code JavaExpression} of {@code expression}
     * @throws JavaExpressionParseException if {@code expression} cannot be parsed
     */
    static JavaExpression atClassDecl(
            String expression, TypeElement classDecl, SourceChecker checker)
            throws JavaExpressionParseException {
        return JavaExpressionParseUtil.parse(expression, classDecl, checker);
    }

    static JavaExpression atMethodDecl(
            String expression, ExecutableElement ee, SourceChecker checker)
            throws JavaExpressionParseException {
        return JavaExpressionParseUtil.parse(expression, ee, checker);
    }

    static JavaExpression atFieldDecl(
            String expression, VariableElement fieldEle, SourceChecker checker)
            throws JavaExpressionParseException {
        return JavaExpressionParseUtil.parse(expression, fieldEle, checker);
    }

    static JavaExpression atMethodBody(
            String expression, MethodTree methodTree, SourceChecker checker)
            throws JavaExpressionParseException {

        ExecutableElement ee = TreeUtils.elementFromDeclaration(methodTree);
        JavaExpression javaExpr = StringToJavaExpression.atMethodDecl(expression, ee, checker);
        return javaExpr.viewpointAdaptAtMethodDecl(methodTree);
    }

    static JavaExpression atMethodInvocation(
            String expression, MethodInvocationTree methodInvocationTree, SourceChecker checker)
            throws JavaExpressionParseException {
        ExecutableElement ee = TreeUtils.elementFromUse(methodInvocationTree);
        JavaExpression javaExpr = StringToJavaExpression.atMethodDecl(expression, ee, checker);
        return javaExpr.viewpointAdaptAtMethodCall(methodInvocationTree);
    }

    static JavaExpression atMethodInvocation(
            String expression, MethodInvocationNode methodInvocationNode, SourceChecker checker)
            throws JavaExpressionParseException {
        ExecutableElement ee = TreeUtils.elementFromUse(methodInvocationNode.getTree());
        JavaExpression javaExpr = StringToJavaExpression.atMethodDecl(expression, ee, checker);
        return javaExpr.viewpointAdaptAtMethodCall(methodInvocationNode);
    }

    static JavaExpression atNewClassTree(
            String expression, NewClassTree newClassTree, SourceChecker checker)
            throws JavaExpressionParseException {
        ExecutableElement ee = TreeUtils.elementFromUse(newClassTree);
        JavaExpression javaExpr = StringToJavaExpression.atMethodDecl(expression, ee, checker);
        return javaExpr.viewpointAdaptAtConstructorCall(newClassTree);
    }

    static JavaExpression atFieldAccess(
            String expression, MemberSelectTree fieldAccess, SourceChecker checker)
            throws JavaExpressionParseException {

        Element ele = TreeUtils.elementFromUse(fieldAccess);
        if (ele.getKind() != ElementKind.FIELD && ele.getKind() != ElementKind.ENUM_CONSTANT) {
            throw new BugInCF("Expected a field, but found %s.", ele.getKind());
        }
        VariableElement fieldEle = (VariableElement) ele;
        JavaExpression receiver = JavaExpression.fromTree(fieldAccess.getExpression());
        JavaExpression javaExpr = StringToJavaExpression.atFieldDecl(expression, fieldEle, checker);
        return javaExpr.viewpointAdaptAtFieldAccess(receiver);
    }

    static JavaExpression atLambdaParameter(
            String expression,
            LambdaExpressionTree lambdaTree,
            TreePath parentPath,
            SourceChecker checker)
            throws JavaExpressionParseException {

        return JavaExpressionParseUtil.parse(expression, lambdaTree, parentPath, checker);
    }

    static JavaExpression atPath(String expression, TreePath path, SourceChecker checker)
            throws JavaExpressionParseException {

        return JavaExpressionParseUtil.parse(expression, path, checker);
    }
}
