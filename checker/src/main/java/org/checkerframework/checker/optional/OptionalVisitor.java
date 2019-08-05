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

    private final TypeMirror collectionType;

    /** The element for java.util.Optional.get(). */
    private final ExecutableElement optionalGet;
    /** The element for java.util.Optional.isPresent(). */
    private final ExecutableElement optionalIsPresent;
    /** The element for java.util.Optional.of(). */
    private final ExecutableElement optionalOf;
    /** The element for java.util.Optional.ofNullable(). */
    private final ExecutableElement optionalOfNullable;
    /** The element for java.util.Optional.orElse(). */
    private final ExecutableElement optionalOrElse;
    /** The element for java.util.Optional.orElseGet(). */
    private final ExecutableElement optionalOrElseGet;
    /** The element for java.util.Optional.orElseThrow(). */
    private final ExecutableElement optionalOrElseThrow;

    /** Create an OptionalVisitor. */
    public OptionalVisitor(BaseTypeChecker checker) {
        super(checker);
        collectionType = types.erasure(TypesUtils.typeFromClass(Collection.class, types, elements));

        ProcessingEnvironment env = checker.getProcessingEnvironment();
        optionalGet = TreeUtils.getMethod("java.util.Optional", "get", 0, env);
        optionalIsPresent = TreeUtils.getMethod("java.util.Optional", "isPresent", 0, env);
        optionalOf = TreeUtils.getMethod("java.util.Optional", "of", 1, env);
        optionalOfNullable = TreeUtils.getMethod("java.util.Optional", "ofNullable", 1, env);
        optionalOrElse = TreeUtils.getMethod("java.util.Optional", "orElse", 1, env);
        optionalOrElseGet = TreeUtils.getMethod("java.util.Optional", "orElseGet", 1, env);
        optionalOrElseThrow = TreeUtils.getMethod("java.util.Optional", "orElseThrow", 1, env);
    }

    @Override
    protected BaseTypeValidator createTypeValidator() {
        return new OptionalTypeValidator(checker, this, atypeFactory);
    }

    /** @return true iff expression is a call to java.util.Optional.get */
    private boolean isCallToGet(ExpressionTree expression) {
        ProcessingEnvironment env = checker.getProcessingEnvironment();
        return TreeUtils.isMethodInvocation(expression, optionalGet, env);
    }

    /** @return true iff expression is a call to java.util.Optional.isPresent */
    private boolean isCallToIsPresent(ExpressionTree expression) {
        ProcessingEnvironment env = checker.getProcessingEnvironment();
        return TreeUtils.isMethodInvocation(expression, optionalIsPresent, env);
    }

    /** @return true iff expression is a call to Optional creation: of, ofNullable. */
    private boolean isOptionalCreation(MethodInvocationTree methInvok) {
        ProcessingEnvironment env = checker.getProcessingEnvironment();
        return TreeUtils.isMethodInvocation(methInvok, optionalOf, env)
                || TreeUtils.isMethodInvocation(methInvok, optionalOfNullable, env);
    }

    /**
     * @return true iff expression is a call to Optional elimination: get, orElse, orElseGet,
     *     orElseThrow
     */
    private boolean isOptionalElimation(MethodInvocationTree methInvok) {
        ProcessingEnvironment env = checker.getProcessingEnvironment();
        return TreeUtils.isMethodInvocation(methInvok, optionalGet, env)
                || TreeUtils.isMethodInvocation(methInvok, optionalOrElse, env)
                || TreeUtils.isMethodInvocation(methInvok, optionalOrElseGet, env)
                || TreeUtils.isMethodInvocation(methInvok, optionalOrElseThrow, env);
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
    // TODO: Should handle this via a transfer function, instead of pattern-matching.
    public void handleTernaryIsPresentGet(ConditionalExpressionTree node) {

        ExpressionTree condExpr = TreeUtils.withoutParens(node.getCondition());
        ExpressionTree trueExpr = TreeUtils.withoutParens(node.getTrueExpression());
        ExpressionTree falseExpr = TreeUtils.withoutParens(node.getFalseExpression());

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
        // Use transfer functions and Store entries.
        if (sameExpression(receiver, getReceiver)) {
            ExecutableElement ele = TreeUtils.elementFromUse((MethodInvocationTree) trueExpr);

            checker.report(
                    Result.warning(
                            "prefer.map.and.orelse",
                            receiver,
                            // The literal "CONTAININGCLASS::" is gross.
                            // TODO: add this to the error message.
                            // ElementUtils.getQualifiedClassName(ele);
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

        ExpressionTree condExpr = TreeUtils.withoutParens(node.getCondition());
        StatementTree thenStmt = skipBlocks(node.getThenStatement());
        StatementTree elseStmt = skipBlocks(node.getElseStatement());

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
        ExpressionTree arg = TreeUtils.withoutParens(args.get(0));
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

        checker.report(Result.warning("introduce.eliminate"), node);
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
                checker.report(Result.warning("optional.field"), node);
            } else if (ekind == ElementKind.PARAMETER) {
                checker.report(Result.warning("optional.parameter"), node);
            }
        }
        return super.visitVariable(node, p);
    }

    /**
     * Handles part of Rule #6, and also Rule #7: Don't permit {@code Collection<Optional<...>>} or
     * {@code Optional<Collection<...>>}.
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
                List<? extends TypeMirror> typeArgs = ((DeclaredType) tm).getTypeArguments();
                if (typeArgs.size() == 1) {
                    // TODO: handle collections that have more than one type parameter
                    TypeMirror typeArg = typeArgs.get(0);
                    if (isOptionalType(typeArg)) {
                        checker.report(Result.warning("optional.as.element.type"), tree);
                    }
                }
            } else if (isOptionalType(tm)) {
                List<? extends TypeMirror> typeArgs = ((DeclaredType) tm).getTypeArguments();
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

    /**
     * If the given tree is a block tree with a single element, return the enclosed non-block
     * statement. Otherwise, return the same tree.
     *
     * @param tree a statement tree
     * @return the single enclosed statement, if it exists; otherwise, the same tree
     */
    // TODO: The Optional Checker should work over the CFG, then it would not need this any longer.
    public static StatementTree skipBlocks(final StatementTree tree) {
        if (tree == null) {
            return tree;
        }
        StatementTree s = tree;
        while (s.getKind() == Tree.Kind.BLOCK) {
            List<? extends StatementTree> stmts = ((BlockTree) s).getStatements();
            if (stmts.size() == 1) {
                s = stmts.get(0);
            } else {
                return s;
            }
        }
        return s;
    }
}
