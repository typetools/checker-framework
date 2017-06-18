package org.checkerframework.checker.optional;

import com.sun.source.tree.*;
import com.sun.source.tree.Tree.Kind;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The OptionalVisitor enforces the Optional Checker rules. These rules are described in detail in
 * the Checker Framework Manual.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 */
public class OptionalVisitor
        extends BaseTypeVisitor</* OptionalAnnotatedTypeFactory*/ BaseAnnotatedTypeFactory> {

    private final ExecutableElement getMethod;
    private final ExecutableElement isPresentMethod;
    private final ExecutableElement ofMethod;
    private final ExecutableElement ofNullableMethod;
    private final ExecutableElement orElseGetMethod;
    private final ExecutableElement orElseMethod;
    private final ExecutableElement orElseThrowMethod;

    public OptionalVisitor(BaseTypeChecker checker) {
        super(checker);
        getMethod = getOptionalMethod("get", 0);
        isPresentMethod = getOptionalMethod("isPresent", 0);
        ofMethod = getOptionalMethod("of", 1);
        ofNullableMethod = getOptionalMethod("ofNullable", 1);
        orElseGetMethod = getOptionalMethod("orElseGet", 1);
        orElseMethod = getOptionalMethod("orElse", 1);
        orElseThrowMethod = getOptionalMethod("orElseThrow", 1);
    }

    @Override
    protected BaseTypeValidator createTypeValidator() {
        return new OptionalTypeValidator(checker, this, atypeFactory);
    }

    private ExecutableElement getOptionalMethod(String methodName, int params) {
        return TreeUtils.getMethod(
                java.util.Optional.class.getName(),
                methodName,
                params,
                atypeFactory.getProcessingEnv());
    }

    /** @return true iff expression is a call to java.util.Optional.get */
    private boolean isCallToGet(ExpressionTree expression) {
        return TreeUtils.isMethodInvocation(expression, getMethod, atypeFactory.getProcessingEnv());
    }

    /** @return true iff expression is a call to java.util.Optional.isPresent */
    private boolean isCallToIsPresent(ExpressionTree expression) {
        return TreeUtils.isMethodInvocation(
                expression, isPresentMethod, atypeFactory.getProcessingEnv());
    }

    // Optional creation: of, ofNullable.
    private boolean isOptionalCreation(MethodInvocationTree methInvok) {
        ExecutableElement invoked = TreeUtils.elementFromUse(methInvok);
        ProcessingEnvironment env = atypeFactory.getProcessingEnv();
        return ElementUtils.isMethod(invoked, ofMethod, env)
                || ElementUtils.isMethod(invoked, ofNullableMethod, env);
    }

    // Optional elimination: get, orElse, orElseGet, orElseThrow.
    private boolean isOptionalElimation(MethodInvocationTree methInvok) {
        ExecutableElement invoked = TreeUtils.elementFromUse(methInvok);
        ProcessingEnvironment env = atypeFactory.getProcessingEnv();
        return ElementUtils.isMethod(invoked, getMethod, env)
                || ElementUtils.isMethod(invoked, orElseMethod, env)
                || ElementUtils.isMethod(invoked, orElseGetMethod, env)
                || ElementUtils.isMethod(invoked, orElseThrowMethod, env);
    }

    /** e is a MethodInvocationTree */
    private ExpressionTree receiver(ExpressionTree e) {
        MethodInvocationTree invok = (MethodInvocationTree) e;
        ExpressionTree methodSelect = invok.getMethodSelect();
        ExpressionTree receiver = ((MemberSelectTree) methodSelect).getExpression();
        return TreeUtils.skipParens(receiver);
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
        handleTernaryIsPresentGet(node);
        return super.visitConditionalExpression(node, p);
    }

    /*
     * Part of rule #3.
     *
     * Pattern match for:  {@code VAR.isPresent() ? VAR.get().METHOD() : VALUE}
     *
     * <p>Prefer:  {@code VAR.map(METHOD).orElse(VALUE);}
     */
    public void handleTernaryIsPresentGet(ConditionalExpressionTree node) {

        ExpressionTree condExpr = TreeUtils.skipParens(node.getCondition());
        ExpressionTree trueExpr = TreeUtils.skipParens(node.getTrueExpression());
        ExpressionTree falseExpr = TreeUtils.skipParens(node.getFalseExpression());

        if (!isCallToIsPresent(condExpr)) {
            return;
        }
        MethodInvocationTree isPresentInvok = (MethodInvocationTree) condExpr;
        ExpressionTree isPresent = isPresentInvok.getMethodSelect();
        if (!(isPresent instanceof MemberSelectTree)) {
            return;
        }
        ExpressionTree receiver =
                TreeUtils.skipParens(((MemberSelectTree) isPresent).getExpression());
        if (!(trueExpr instanceof MethodInvocationTree)) {
            return;
        }
        MethodInvocationTree trueMethodInvok = (MethodInvocationTree) trueExpr;
        List<? extends ExpressionTree> trueArgs = trueMethodInvok.getArguments();
        if (!trueArgs.isEmpty()) {
            return;
        }
        MemberSelectTree trueMethod = (MemberSelectTree) trueMethodInvok.getMethodSelect();
        ExpressionTree trueReceiver = TreeUtils.skipParens(trueMethod.getExpression());
        if (!isCallToGet(trueReceiver)) {
            return;
        }
        ExpressionTree getReceiver = receiver(trueReceiver);

        // What is a better way to do this than string comparison?
        if (receiver.toString().equals(getReceiver.toString())) {
            checker.report(
                    Result.warning(
                            "prefer.map.and.orelse",
                            receiver,
                            // The literal "CONTAININGCLASS::" is gross.
                            // Figure out a way to improve it.
                            trueMethod.getIdentifier(),
                            falseExpr),
                    node);
        }
    }

    @Override
    public Void visitIf(IfTree node, Void p) {
        handleConditionalStatementIsPresentGet(node);
        return super.visitIf(node, p);
    }

    /*
     * Part of rule #3.
     *
     * Pattern match for: {@code if (VAR.isPresent()) { METHOD(VAR.get()); }}
     *
     * <p>Prefer:  {@code VAR.ifPresent(METHOD);}
     */
    public void handleConditionalStatementIsPresentGet(IfTree node) {

        ExpressionTree condExpr = TreeUtils.skipParens(node.getCondition());
        StatementTree thenStmt = TreeUtils.skipBlocks(node.getThenStatement());
        StatementTree elseStmt = TreeUtils.skipBlocks(node.getElseStatement());

        if (!(elseStmt == null
                || (elseStmt.getKind() == Tree.Kind.BLOCK
                        && ((BlockTree) elseStmt).getStatements().isEmpty()))) {
            // else block is missing or is an empty block: "{}"
            return;
        }
        if (!isCallToIsPresent(condExpr)) {
            return;
        }
        ExpressionTree receiver = receiver(condExpr);
        if (thenStmt.getKind() != Kind.EXPRESSION_STATEMENT) {
            return;
        }
        ExpressionTree thenExpr = ((ExpressionStatementTree) thenStmt).getExpression();
        if (thenExpr.getKind() != Kind.METHOD_INVOCATION) {
            return;
        }
        MethodInvocationTree invok = (MethodInvocationTree) thenExpr;
        List<? extends ExpressionTree> args = invok.getArguments();
        if (args.size() != 1) {
            return;
        }
        ExpressionTree arg = TreeUtils.skipParens(args.get(0));
        if (!isCallToGet(arg)) {
            return;
        }
        ExpressionTree getReceiver = receiver(arg);
        if (!receiver.toString().equals(getReceiver.toString())) {
            return;
        }
        ExpressionTree method = invok.getMethodSelect();

        String methodString = method.toString();
        int dotPos = methodString.lastIndexOf(".");
        if (dotPos != -1) {
            methodString =
                    methodString.substring(0, dotPos) + "::" + methodString.substring(dotPos + 1);
        }

        checker.report(Result.warning("prefer.ifpresent", receiver, methodString), node);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        handleCreationElimination(node);
        return super.visitMethodInvocation(node, p);
    }

    /**
     * Rule #4.
     *
     * <p>Pattern match for: {@code CREATION().ELIMINATION()}
     *
     * <p>Prefer: {@code VAR.ifPresent(METHOD);}
     */
    public void handleCreationElimination(MethodInvocationTree node) {
        if (!isOptionalElimation(node)) {
            return;
        }
        ExpressionTree receiver = receiver(node);
        if (!((receiver instanceof MethodInvocationTree)
                && isOptionalCreation((MethodInvocationTree) receiver))) {
            return;
        }

        checker.report(Result.warning("introduce.eliminate.optional"), node);
    }

    /**
     * Rule #6 (partial).
     *
     * <p>Don't use Optional in fields and method parameters.
     */
    @Override
    public Void visitVariable(VariableTree node, Void p) {
        VariableElement ve = TreeUtils.elementFromDeclaration(node);
        TypeMirror tm = ve.asType();
        if (isOptionalType(tm, null)) {
            ElementKind ekind = TreeUtils.elementFromDeclaration(node).getKind();
            if (ekind.isField()) {
                checker.report(Result.failure("optional.field"), node);
            } else if (ekind == ElementKind.PARAMETER) {
                checker.report(Result.failure("optional.parameter"), node);
            }
        }
        return super.visitVariable(node, p);
    }

    /** Handles part of Rule #6, and also Rule #7. */
    private final class OptionalTypeValidator extends BaseTypeValidator {

        public OptionalTypeValidator(
                BaseTypeChecker checker,
                BaseTypeVisitor<?> visitor,
                AnnotatedTypeFactory atypeFactory) {
            super(checker, visitor, atypeFactory);
        }

        // TODO: Why is "isValid" called twice on the right-hand-side of a variable initializer?
        // It leads to the error being issued twice.
        /**
         * Rules 6 (partial) and 7: Don't permit {@code Collection<Optional<...>>} or {@code
         * Optional<Collection<...>>}.
         */
        @Override
        public boolean isValid(AnnotatedTypeMirror type, Tree tree) {
            TypeMirror tm = type.getUnderlyingType();
            if (isCollectionType(tm, tree)) {
                DeclaredType type1 = (DeclaredType) (type.getUnderlyingType());
                List<? extends TypeMirror> typeArgs = type1.getTypeArguments();
                if (typeArgs.size() == 1) {
                    // TODO: handle collections that have more than one type parameter
                    TypeMirror typeArg = typeArgs.get(0);
                    if (isOptionalType(typeArg, null)) {
                        checker.report(Result.failure("optional.as.element.type"), tree);
                    }
                }
            } else if (isOptionalType(tm, tree)) {
                List<? extends TypeMirror> typeArgs =
                        ((DeclaredType) (type.getUnderlyingType())).getTypeArguments();
                assert typeArgs.size() == 1;
                TypeMirror typeArg = typeArgs.get(0);
                if (isCollectionType(typeArg, null)) {
                    checker.report(Result.failure("optional.collection"), tree);
                }
            }
            return super.isValid(type, tree);
        }
    }

    private boolean isCollectionType(TypeMirror tm, Tree tree) {
        if (tm.getKind() == TypeKind.DECLARED) {
            DeclaredType type1 = (DeclaredType) tm;
            TypeElement elt1 = (TypeElement) type1.asElement();
            List<TypeElement> superTypes = ElementUtils.getSuperTypes(elements, elt1);
            for (TypeElement superType : superTypes) {
                // For some reason, testing superType.getQualifiedName() does not work here
                // even though it prints idenically
                if (superType.getQualifiedName().toString().equals("java.util.Collection")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isOptionalType(TypeMirror tm, Tree tree) {
        if (tm.getKind() == TypeKind.DECLARED) {
            DeclaredType type1 = (DeclaredType) tm;
            TypeElement elt1 = (TypeElement) type1.asElement();
            return elt1.getQualifiedName().toString().equals("java.util.Optional");
        }
        return false;
    }
}
