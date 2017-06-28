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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.NullnessAnnotatedTypeFactory;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
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

    public OptionalVisitor(BaseTypeChecker checker) {
        super(checker);
        collectionType = types.erasure(TypesUtils.typeFromClass(types, elements, Collection.class));
    }

    /** Provides a way to query the Nullness Checker. */
    NullnessAnnotatedTypeFactory getNullnessTypeFactory() {
        return checker.getTypeFactoryOfSubchecker(NullnessChecker.class);
    }

    @Override
    protected BaseTypeValidator createTypeValidator() {
        return new OptionalTypeValidator(checker, this, atypeFactory);
    }

    /** @return true iff expression is a call to java.util.Optional.get */
    private boolean isCallToGet(ExpressionTree expression) {
        return OptionalUtils.isMethodInvocation(expression, "get", 0, atypeFactory);
    }

    /** @return true iff expression is a call to java.util.Optional.isPresent */
    private boolean isCallToIsPresent(ExpressionTree expression) {
        return OptionalUtils.isMethodInvocation(expression, "isPresent", 0, atypeFactory);
    }

    /** @return true iff expression is a call to java.util.Optional.of */
    private boolean isCallToOf(ExpressionTree expression) {
        return OptionalUtils.isMethodInvocation(expression, "of", 1, atypeFactory);
    }

    /** @return true iff expression is a call to Optional creation: of, ofNullable. */
    private boolean isOptionalCreation(MethodInvocationTree methInvok) {
        return OptionalUtils.isMethodInvocation(methInvok, "of", 1, atypeFactory)
                || OptionalUtils.isMethodInvocation(methInvok, "ofNullable", 1, atypeFactory);
    }

    /**
     * @return true iff expression is a call to Optional elimination: get, orElse, orElseGet,
     *     orElseThrow.
     */
    private boolean isOptionalElimation(MethodInvocationTree methInvok) {
        return OptionalUtils.isMethodInvocation(methInvok, "get", 0, atypeFactory)
                || OptionalUtils.isMethodInvocation(methInvok, "orElse", 1, atypeFactory)
                || OptionalUtils.isMethodInvocation(methInvok, "orElseGet", 1, atypeFactory)
                || OptionalUtils.isMethodInvocation(methInvok, "orElseThrow", 1, atypeFactory);
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
        handleOfRedundantly(node);
        return super.visitMethodInvocation(node, p);
    }

    /**
     * Redundantly force the argument of {@code Optional.of} to be non-null.
     *
     * <p>This is enforced by the JDK annotations for the Nullness Checker, which is run as a
     * subchecker of the Optional Checker. However, this error is issued even if a user suppresses
     * all nullness warnings. A user may suppress this warning as well if desired.
     */
    public void handleOfRedundantly(MethodInvocationTree node) {
        if (!isCallToOf(node)) {
            return;
        }

        // Determine the Nullness annotation on the argument.
        ExpressionTree arg0 = node.getArguments().get(0);
        final AnnotatedTypeMirror argNullnessType = getNullnessTypeFactory().getAnnotatedType(arg0);
        boolean argIsNonNull =
                AnnotationUtils.containsSameByClass(
                        argNullnessType.getAnnotations(), NonNull.class);
        if (!argIsNonNull) {
            checker.report(Result.failure("of.nullable.argument"), arg0);
        }
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
                        checker.report(Result.warning("optional.as.element.type"), tree);
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
