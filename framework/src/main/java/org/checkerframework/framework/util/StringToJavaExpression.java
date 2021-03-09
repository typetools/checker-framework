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
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

// TODO: Regarding "static methods that convert", the first several are documented as "parses",
// which implies no conversion.
// TODO: Is there any transformation of interest other than viewpoint-adaptation?  Giving at least
// that example will make the documentation more concrete and therefore easier to understand.  The
// same comment applies to later documentation that mentions transformation.
// TODO: Does "parsing at a location" inherently include transformation?
/**
 * This interface is both a functional interface, see {@link #toJavaExpression(String)}, and also a
 * collection of static methods that convert a string to a JavaExpression at common locations.
 * Conversion includes parsing {@code stringExpr} to a {@code JavaExpression} and optionally
 * transforming the result of parsing into another {@code JavaExpression}. To parse a string "at a
 * location" means to parse it as if it were written in an annotation that is written on that
 * location.
 */
@FunctionalInterface
public interface StringToJavaExpression {

    /**
     * Convert a string to a {@link JavaExpression}.
     *
     * <p>If no conversion exists, {@code null} is returned.
     *
     * <p>Conversion includes parsing {@code stringExpr} to a {@code JavaExpression} and optionally
     * transforming the result of parsing into another {@code JavaExpression}.
     *
     * @param stringExpr a Java expression
     * @return a {@code JavaExpression} or {@code null} if no conversion from {@code stringExpr}
     *     exists
     * @throws JavaExpressionParseException if {@code stringExpr} cannot be parsed to a {@code
     *     JavaExpression}
     */
    @Nullable JavaExpression toJavaExpression(String stringExpr) throws JavaExpressionParseException;

    // TODO: "@return the" implies that there is one canonical one that will always be returned.  I
    // suggest rewording throughout.  (The above method does not have this issue.)
    /**
     * Parses a string to a {@link JavaExpression} as if it were written at {@code typeElement}.
     *
     * @param expression a Java expression
     * @param typeElement type element at which {@code expression} is parsed
     * @param checker checker used to get the {@link
     *     javax.annotation.processing.ProcessingEnvironment} and current {@link
     *     com.sun.source.tree.CompilationUnitTree}
     * @return the {@code JavaExpression} for {@code expression}
     * @throws JavaExpressionParseException if {@code expression} cannot be parsed
     */
    static JavaExpression atTypeDecl(
            String expression, TypeElement typeElement, SourceChecker checker)
            throws JavaExpressionParseException {
        ThisReference thisReference = new ThisReference(typeElement.asType());
        List<FormalParameter> parameters = null;
        // TODO: This call explains why JavaExpressionParseUtil.parse is not private.  I suggest it
        // should be package-private.
        // TODO: Should there be a variant of parse that takes a SourceChecker instead of its
        // current two final formal parameters?  That would simplify all these calls.
        return JavaExpressionParseUtil.parse(
                expression,
                typeElement.asType(),
                thisReference,
                parameters,
                null,
                checker.getPathToCompilationUnit(),
                checker.getProcessingEnvironment());
    }

    /**
     * Parses a string to a {@link JavaExpression} as if it were written at {@code fieldElement}.
     *
     * @param expression a Java expression
     * @param fieldElement variable element at which {@code expression} is parsed
     * @param checker checker used to get the {@link
     *     javax.annotation.processing.ProcessingEnvironment} and current {@link
     *     com.sun.source.tree.CompilationUnitTree}
     * @return the {@code JavaExpression} for {@code expression}
     * @throws JavaExpressionParseException if {@code expression} cannot be parsed
     */
    static JavaExpression atFieldDecl(
            String expression, VariableElement fieldElement, SourceChecker checker)
            throws JavaExpressionParseException {
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
                checker.getPathToCompilationUnit(),
                checker.getProcessingEnvironment());
    }

    /**
     * Parses a string to a {@link JavaExpression} as if it were written at {@code method}. The
     * returned {@code JavaExpression} uses {@link FormalParameter}s to represent parameters. Use
     * {@link #atMethodBody(String, MethodTree, SourceChecker)} if parameters should be {@link
     * LocalVariable}s instead.
     *
     * @param expression a Java expression
     * @param method method element at which {@code expression} is parsed
     * @param checker checker used to get the {@link
     *     javax.annotation.processing.ProcessingEnvironment} and current {@link
     *     com.sun.source.tree.CompilationUnitTree}
     * @return the {@code JavaExpression} for {@code expression}
     * @throws JavaExpressionParseException if {@code expression} cannot be parsed
     */
    static JavaExpression atMethodDecl(
            String expression, ExecutableElement method, SourceChecker checker)
            throws JavaExpressionParseException {
        TypeMirror enclosingType = ElementUtils.enclosingTypeElement(method).asType();
        ThisReference thisReference;
        if (ElementUtils.isStatic(method)) {
            // Can't use "this" on a static method
            thisReference = null;
        } else {
            thisReference = new ThisReference(enclosingType);
        }
        List<FormalParameter> parameters = JavaExpression.getFormalParameters(method);
        return JavaExpressionParseUtil.parse(
                expression,
                enclosingType,
                thisReference,
                parameters,
                null,
                checker.getPathToCompilationUnit(),
                checker.getProcessingEnvironment());
    }

    /**
     * Parses a string to a {@link JavaExpression} as if it were written at {@code methodTree}. The
     * returned {@code JavaExpression} uses {@link LocalVariable}s to represent parameters. Use
     * {@link #atMethodDecl(String, ExecutableElement, SourceChecker)} if parameters should be
     * {@link FormalParameter}s instead.
     *
     * @param expression a Java expression
     * @param methodTree method declaration tree at which {@code expression} is parsed
     * @param checker checker used to get the {@link
     *     javax.annotation.processing.ProcessingEnvironment} and current {@link
     *     com.sun.source.tree.CompilationUnitTree}
     * @return the {@code JavaExpression} for {@code expression}
     * @throws JavaExpressionParseException if {@code expression} cannot be parsed
     */
    static JavaExpression atMethodBody(
            String expression, MethodTree methodTree, SourceChecker checker)
            throws JavaExpressionParseException {

        ExecutableElement ee = TreeUtils.elementFromDeclaration(methodTree);
        JavaExpression javaExpr = StringToJavaExpression.atMethodDecl(expression, ee, checker);
        return javaExpr.atMethodBody(methodTree);
    }

    /**
     * Parses a string as if it were written at the declaration of the invoked method and then
     * viewpoint-adapts the result to the call site.
     *
     * @param expression a Java expression
     * @param methodInvocationTree method invocation tree
     * @param checker checker used to get the {@link
     *     javax.annotation.processing.ProcessingEnvironment} and current {@link
     *     com.sun.source.tree.CompilationUnitTree}
     * @return the {@code JavaExpression} for {@code expression}
     * @throws JavaExpressionParseException if {@code expression} cannot be parsed
     */
    static JavaExpression atMethodInvocation(
            String expression, MethodInvocationTree methodInvocationTree, SourceChecker checker)
            throws JavaExpressionParseException {
        ExecutableElement ee = TreeUtils.elementFromUse(methodInvocationTree);
        JavaExpression javaExpr = StringToJavaExpression.atMethodDecl(expression, ee, checker);
        return javaExpr.atMethodInvocation(methodInvocationTree);
    }

    /**
     * Parses a string as if it were written at the declaration of the invoked method and then
     * viewpoint-adapts the result to the call site.
     *
     * @param expression a Java expression
     * @param methodInvocationNode method invocation node
     * @param checker checker used to get the {@link
     *     javax.annotation.processing.ProcessingEnvironment} and current {@link
     *     com.sun.source.tree.CompilationUnitTree}
     * @return the {@code JavaExpression} for {@code expression}
     * @throws JavaExpressionParseException if {@code expression} cannot be parsed
     */
    static JavaExpression atMethodInvocation(
            String expression, MethodInvocationNode methodInvocationNode, SourceChecker checker)
            throws JavaExpressionParseException {
        ExecutableElement ee = TreeUtils.elementFromUse(methodInvocationNode.getTree());
        JavaExpression javaExpr = StringToJavaExpression.atMethodDecl(expression, ee, checker);
        return javaExpr.atMethodInvocation(methodInvocationNode);
    }

    /**
     * Parses a string as if it were written at the declaration of the invoked constructor and then
     * viewpoint-adapts the result to the call site.
     *
     * @param expression a Java expression
     * @param newClassTree constructor invocation
     * @param checker checker used to get the {@link
     *     javax.annotation.processing.ProcessingEnvironment} and current {@link
     *     com.sun.source.tree.CompilationUnitTree}
     * @return the {@code JavaExpression} for {@code expression}
     * @throws JavaExpressionParseException if {@code expression} cannot be parsed
     */
    static JavaExpression atConstructorInvocation(
            String expression, NewClassTree newClassTree, SourceChecker checker)
            throws JavaExpressionParseException {
        ExecutableElement ee = TreeUtils.elementFromUse(newClassTree);
        JavaExpression javaExpr = StringToJavaExpression.atMethodDecl(expression, ee, checker);
        return javaExpr.atConstructorInvocation(newClassTree);
    }

    /**
     * Parses a string as if it were written at the declaration the field and then viewpoint-adapts
     * the result to the use.
     *
     * @param expression a Java expression
     * @param fieldAccess the field access tree
     * @param checker checker used to get the {@link
     *     javax.annotation.processing.ProcessingEnvironment} and current {@link
     *     com.sun.source.tree.CompilationUnitTree}
     * @return the {@code JavaExpression} for {@code expression}
     * @throws JavaExpressionParseException if {@code expression} cannot be parsed
     */
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
        return javaExpr.atFieldAccess(receiver);
    }

    /**
     * Parses a string as if it were written at the one of the parameters of {@code lambdaTree}.
     * Parameters of the lambda are expressed as {@link LocalVariable}s.
     *
     * @param expression a Java expression
     * @param lambdaTree the lambda tree
     * @param parentPath path to the parent of {@code lambdaTree}; required because the expression
     *     can reference final local variables of the enclosing method
     * @param checker checker used to get the {@link
     *     javax.annotation.processing.ProcessingEnvironment} and current {@link
     *     com.sun.source.tree.CompilationUnitTree}
     * @return the {@code JavaExpression} for {@code expression}
     * @throws JavaExpressionParseException if {@code expression} cannot be parsed
     */
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
        List<JavaExpression> paramsAsLocals = new ArrayList<>(lambdaTree.getParameters().size());
        List<FormalParameter> parameters = new ArrayList<>(lambdaTree.getParameters().size());
        int oneBasedIndex = 1;
        for (VariableTree arg : lambdaTree.getParameters()) {
            LocalVariable param = (LocalVariable) JavaExpression.fromVariableTree(arg);
            parameters.add(
                    new FormalParameter(oneBasedIndex, (VariableElement) param.getElement()));
            paramsAsLocals.add(param);
            oneBasedIndex++;
        }

        JavaExpression javaExpr =
                JavaExpressionParseUtil.parse(
                        expression,
                        enclosingType,
                        thisReference,
                        parameters,
                        parentPath,
                        checker.getPathToCompilationUnit(),
                        checker.getProcessingEnvironment());
        return ViewpointAdaptJavaExpression.viewpointAdapt(javaExpr, paramsAsLocals);
    }

    /**
     * Parses a string as if it were written at {@code localVarPath}.
     *
     * @param expression a Java expression
     * @param localVarPath location at which {@code expression} is parsed
     * @param checker checker used to get the {@link
     *     javax.annotation.processing.ProcessingEnvironment} and current {@link
     *     com.sun.source.tree.CompilationUnitTree}
     * @return the {@code JavaExpression} for {@code expression}
     * @throws JavaExpressionParseException if {@code expression} cannot be parsed
     */
    static JavaExpression atPath(String expression, TreePath localVarPath, SourceChecker checker)
            throws JavaExpressionParseException {

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
                    checker.getPathToCompilationUnit(),
                    checker.getProcessingEnvironment());
        }

        ExecutableElement methodEle = TreeUtils.elementFromDeclaration(methodTree);
        List<FormalParameter> parameters = JavaExpression.getFormalParameters(methodEle);
        JavaExpression javaExpr =
                JavaExpressionParseUtil.parse(
                        expression,
                        enclosingType,
                        thisReference,
                        parameters,
                        localVarPath,
                        checker.getPathToCompilationUnit(),
                        checker.getProcessingEnvironment());
        List<JavaExpression> paramsAsLocals =
                JavaExpression.getParametersAsLocalVariables(methodEle);
        return ViewpointAdaptJavaExpression.viewpointAdapt(javaExpr, paramsAsLocals);
    }
}
