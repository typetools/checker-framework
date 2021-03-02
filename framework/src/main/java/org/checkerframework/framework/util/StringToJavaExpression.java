package org.checkerframework.framework.util;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FormalParameter;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.expression.ViewpointAdaptJavaExpression;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
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
     * <p>Conversion includes parsing {@code stringExpr} to a {@code JavaExpression} and optionally
     * transforming the result of parsing into an other {@code JavaExpression}.
     *
     * @param stringExpr an string expression
     * @return a {@code JavaExpression} or {@code null} if no conversion from {@code stringExpr}
     *     exists
     * @throws JavaExpressionParseException if {@code stringExpr} cannot be parsed to a {@code
     *     JavaExpression}
     */
    @Nullable JavaExpression toJavaExpression(String stringExpr) throws JavaExpressionParseException;

    static JavaExpression atMethodDecl(
            String expression, ExecutableElement method, SourceChecker checker)
            throws JavaExpressionParseException {
        // The underlying javac API used to convert from Strings to Elements requires a tree path
        // even when the information could be deduced from elements alone.  So use the path to the
        // current CompilationUnit.
        TreePath pathToCompilationUnit = checker.getPathToCompilationUnit();
        ProcessingEnvironment env = checker.getProcessingEnvironment();
        TypeMirror enclosingType = ElementUtils.enclosingTypeElement(method).asType();
        ThisReference thisReference;
        if (ElementUtils.isStatic(method)) {
            // Can't use "this" on a static method
            thisReference = null;
        } else {
            thisReference = new ThisReference(enclosingType);
        }
        List<FormalParameter> parameters = getFormalParameters(method);
        return JavaExpressionParseUtil.parse(
                expression,
                enclosingType,
                thisReference,
                parameters,
                null,
                pathToCompilationUnit,
                env);
    }

    static JavaExpression atFieldDecl(
            String expression, VariableElement fieldElement, SourceChecker checker)
            throws JavaExpressionParseException {
        // The underlying javac API used to convert from Strings to Elements requires a tree path
        // even when the information could be deduced from elements alone.  So use the path to the
        // current CompilationUnit.
        TreePath pathToCompilationUnit = checker.getPathToCompilationUnit();
        ProcessingEnvironment env = checker.getProcessingEnvironment();
        TypeMirror enclosingType = ElementUtils.enclosingTypeElement(fieldElement).asType();
        ThisReference thisReference;
        if (ElementUtils.isStatic(fieldElement)) {
            // Can't use "this" on a static fieldElement
            thisReference = null;
        } else {
            thisReference = new ThisReference(enclosingType);
        }
        List<FormalParameter> parameters = null;
        return JavaExpressionParseUtil.parse(
                expression,
                enclosingType,
                thisReference,
                parameters,
                null,
                pathToCompilationUnit,
                env);
    }

    /**
     * Parses {@code expression} to a {@link JavaExpression} as if it were written at {@code
     * typeElement}.
     *
     * @param expression a string expression
     * @param typeElement type element at which {@code expression} is parsed.
     * @param checker checker used to get the {@link
     *     javax.annotation.processing.ProcessingEnvironment} and current {@link
     *     com.sun.source.tree.CompilationUnitTree}
     * @return the {@code JavaExpression} of {@code expression}
     * @throws JavaExpressionParseException if {@code expression} cannot be parsed
     */
    static JavaExpression atClassDecl(
            String expression, TypeElement typeElement, SourceChecker checker)
            throws JavaExpressionParseException {
        // The underlying javac API used to convert from Strings to Elements requires a tree path
        // even when the information could be deduced from elements alone.  So use the path to the
        // current CompilationUnit.
        TreePath pathToCompilationUnit = checker.getPathToCompilationUnit();
        ProcessingEnvironment env = checker.getProcessingEnvironment();
        ThisReference thisReference = new ThisReference(typeElement.asType());

        List<FormalParameter> parameters = null;
        return JavaExpressionParseUtil.parse(
                expression,
                typeElement.asType(),
                thisReference,
                parameters,
                null,
                pathToCompilationUnit,
                env);
    }

    static JavaExpression atLambdaParameter(
            String expression,
            LambdaExpressionTree lambdaTree,
            TreePath parentPath,
            SourceChecker checker)
            throws JavaExpressionParseException {

        TypeMirror enclosingType = TreeUtils.typeOf(TreePathUtil.enclosingClass(parentPath));
        JavaExpression receiver = JavaExpression.getPseudoReceiver(parentPath, enclosingType);
        ThisReference thisReference =
                receiver instanceof ClassName ? null : (ThisReference) receiver;
        List<JavaExpression> paramsAsLocals = new ArrayList<>();
        List<FormalParameter> parameters = new ArrayList<>();
        int oneBasedIndex = 1;
        for (VariableTree arg : lambdaTree.getParameters()) {
            LocalVariable param = (LocalVariable) JavaExpression.fromVariableTree(arg);
            parameters.add(
                    new FormalParameter(oneBasedIndex, (VariableElement) param.getElement()));
            paramsAsLocals.add(param);
            oneBasedIndex++;
        }
        TreePath pathToCompilationUnit = checker.getPathToCompilationUnit();
        ProcessingEnvironment env = checker.getProcessingEnvironment();

        JavaExpression javaExpr =
                JavaExpressionParseUtil.parse(
                        expression,
                        enclosingType,
                        thisReference,
                        parameters,
                        parentPath,
                        pathToCompilationUnit,
                        env);
        return ViewpointAdaptJavaExpression.viewpointAdapt(javaExpr, paramsAsLocals);
    }

    static JavaExpression atPath(String expression, TreePath localVarPath, SourceChecker checker)
            throws JavaExpressionParseException {
        // The underlying javac API used to convert from Strings to Elements requires a TreePath
        // even when the information could be deduced from elements alone.  So use the TreePath to
        // the
        // current CompilationUnit.
        TreePath pathToCompilationUnit = checker.getPathToCompilationUnit();
        ProcessingEnvironment env = checker.getProcessingEnvironment();

        TypeMirror enclosingType = TreeUtils.typeOf(TreePathUtil.enclosingClass(localVarPath));
        ThisReference thisReference = null;
        if (!TreePathUtil.isTreeInStaticScope(localVarPath)) {
            thisReference = new ThisReference(enclosingType);
        }

        MethodTree methodTree = TreePathUtil.enclosingMethod(localVarPath);
        if (methodTree == null) {
            return JavaExpressionParseUtil.parse(
                    expression,
                    enclosingType,
                    thisReference,
                    null,
                    localVarPath,
                    pathToCompilationUnit,
                    env);
        }

        ExecutableElement methodEle = TreeUtils.elementFromDeclaration(methodTree);
        List<FormalParameter> parameters = getFormalParameters(methodEle);
        JavaExpression javaExpr =
                JavaExpressionParseUtil.parse(
                        expression,
                        enclosingType,
                        thisReference,
                        parameters,
                        localVarPath,
                        pathToCompilationUnit,
                        env);
        List<JavaExpression> paramsAsLocals = getParametersAsLocalVars(methodEle);
        return ViewpointAdaptJavaExpression.viewpointAdapt(javaExpr, paramsAsLocals);
    }

    static List<JavaExpression> getParametersAsLocalVars(ExecutableElement methodEle) {
        List<JavaExpression> parameters = new ArrayList<>();
        for (VariableElement variableElement : methodEle.getParameters()) {
            LocalVariable parameter = new LocalVariable(variableElement);
            parameters.add(parameter);
        }
        return parameters;
    }

    static List<FormalParameter> getFormalParameters(ExecutableElement methodEle) {
        List<FormalParameter> parameters = new ArrayList<>();
        int oneBasedIndex = 1;
        for (VariableElement variableElement : methodEle.getParameters()) {
            FormalParameter parameter = new FormalParameter(oneBasedIndex, variableElement);
            parameters.add(parameter);
            oneBasedIndex++;
        }
        return parameters;
    }

    static JavaExpression atMethodBody(
            String expression, MethodTree methodTree, SourceChecker checker)
            throws JavaExpressionParseException {

        ExecutableElement ee = TreeUtils.elementFromDeclaration(methodTree);
        JavaExpression javaExpr = StringToJavaExpression.atMethodDecl(expression, ee, checker);
        return javaExpr.viewpointAdaptAtMethodBody(methodTree);
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
}
