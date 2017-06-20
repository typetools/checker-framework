package org.checkerframework.checker.optional;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import java.util.Collection;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The OptionalVisitor enforces the Optional Checker rules. These rules are described in the Checker
 * Framework Manual.
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

    private final TypeMirror collectionType;

    public OptionalVisitor(BaseTypeChecker checker) {
        super(checker);
        getMethod = getOptionalMethod("get", 0);
        isPresentMethod = getOptionalMethod("isPresent", 0);
        ofMethod = getOptionalMethod("of", 1);
        ofNullableMethod = getOptionalMethod("ofNullable", 1);
        orElseGetMethod = getOptionalMethod("orElseGet", 1);
        orElseMethod = getOptionalMethod("orElse", 1);
        orElseThrowMethod = getOptionalMethod("orElseThrow", 1);
        collectionType = types.erasure(TypesUtils.typeFromClass(types, elements, Collection.class));
    }

    @Override
    protected BaseTypeValidator createTypeValidator() {
        return new OptionalTypeValidator(checker, this, atypeFactory);
    }

    private ExecutableElement getOptionalMethod(String methodName, int params) {
        if (elements.getTypeElement("java.util.Optional") == null) {
            ErrorReporter.errorAbort("The Optional Checker requires Java 8.");
        }
        return TreeUtils.getMethod(
                "java.util.Optional", methodName, params, atypeFactory.getProcessingEnv());
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

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
        handleTernaryIsPresentGet(node);
        return super.visitConditionalExpression(node, p);
    }

    /**
     * Part of rule #3.
     *
     * <p>Pattern match for: {@code VAR.isPresent() ? VAR.get().METHOD() : VALUE}
     *
     * <p>Prefer: {@code VAR.map(METHOD).orElse(VALUE);}
     */
    public void handleTernaryIsPresentGet(ConditionalExpressionTree node) {

        ExpressionTree condExpr = TreeUtils.skipParens(node.getCondition());
        ExpressionTree trueExpr = TreeUtils.skipParens(node.getTrueExpression());
        ExpressionTree falseExpr = TreeUtils.skipParens(node.getFalseExpression());

        if (!isCallToIsPresent(condExpr)) {
            return;
        }
        ExpressionTree receiver = TreeUtils.getReceiverTree(condExpr);

        if (trueExpr.getKind() != Kind.METHOD_INVOCATION) {
            return;
        }
        ExpressionTree trueReceiver = TreeUtils.getReceiverTree(trueExpr);
        if (!isCallToGet(trueReceiver)) {
            return;
        }
        ExpressionTree getReceiver = TreeUtils.getReceiverTree(trueReceiver);

        // What is a better way to do this than string comparison?
        if (receiver.toString().equals(getReceiver.toString())) {
            ExecutableElement ele = TreeUtils.elementFromUse((MethodInvocationTree) trueExpr);

            checker.report(
                    Result.warning(
                            "prefer.map.and.orelse",
                            receiver,
                            // The literal "CONTAININGCLASS::" is gross.
                            // TODO: add this to the error message.
                            //ElementUtils.getQualifiedClassName(ele);
                            ele.getSimpleName(),
                            falseExpr),
                    node);
        }
    }

    /** Return true if the two trees represent the same expression. */
    private boolean sameExpression(ExpressionTree tree1, ExpressionTree tree2) {
        FlowExpressions.Receiver r1 = FlowExpressions.internalReprOf(atypeFactory, tree1);
        FlowExpressions.Receiver r2 = FlowExpressions.internalReprOf(atypeFactory, tree1);
        if (r1 != null && !r1.containsUnknown() && r2 != null && !r2.containsUnknown()) {
            return r1.equals(r2);
        } else {
            return tree1.toString().equals(tree2.toString());
        }
    }

    @Override
    public Void visitIf(IfTree node, Void p) {
        handleConditionalStatementIsPresentGet(node);
        return super.visitIf(node, p);
    }

    /**
     * Part of rule #3.
     *
     * <p>Pattern match for: {@code if (VAR.isPresent()) { METHOD(VAR.get()); }}
     *
     * <p>Prefer: {@code VAR.ifPresent(METHOD);}
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
        ExpressionTree receiver = TreeUtils.getReceiverTree(condExpr);
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
        ExpressionTree getReceiver = TreeUtils.getReceiverTree(arg);
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
        ExpressionTree receiver = TreeUtils.getReceiverTree(node);
        if (!(receiver.getKind() == Kind.METHOD_INVOCATION
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
        if (isOptionalType(tm)) {
            ElementKind ekind = TreeUtils.elementFromDeclaration(node).getKind();
            if (ekind.isField()) {
                checker.report(Result.failure("optional.field"), node);
            } else if (ekind == ElementKind.PARAMETER) {
                checker.report(Result.failure("optional.parameter"), node);
            }
        }
        return super.visitVariable(node, p);
    }

    /**
     * Handles part of Rule #6, and also Rule #7: Don't permit {@code Collection<Optional<...>>} or
     * {@code Optional<Collection<...>>}..
     */
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
            if (isCollectionType(tm)) {
                DeclaredType type1 = (DeclaredType) (type.getUnderlyingType());
                List<? extends TypeMirror> typeArgs = type1.getTypeArguments();
                if (typeArgs.size() == 1) {
                    // TODO: handle collections that have more than one type parameter
                    TypeMirror typeArg = typeArgs.get(0);
                    if (isOptionalType(typeArg)) {
                        checker.report(Result.failure("optional.as.element.type"), tree);
                    }
                }
            } else if (isOptionalType(tm)) {
                List<? extends TypeMirror> typeArgs =
                        ((DeclaredType) (type.getUnderlyingType())).getTypeArguments();
                assert typeArgs.size() == 1;
                TypeMirror typeArg = typeArgs.get(0);
                if (isCollectionType(typeArg)) {
                    checker.report(Result.failure("optional.collection"), tree);
                }
            }
            return super.isValid(type, tree);
        }
    }

    /** Return true if tm represents a subtype of Collection (other than the Null type). */
    private boolean isCollectionType(TypeMirror tm) {
        return tm.getKind() == TypeKind.DECLARED && types.isSubtype(tm, collectionType);
    }

    /** Return true if tm represents java.util.Optional. */
    private boolean isOptionalType(TypeMirror tm) {
        return TypesUtils.isDeclaredOfName(tm, "java.util.Optional");
    }
}
