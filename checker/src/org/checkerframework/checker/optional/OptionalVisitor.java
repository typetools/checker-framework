package org.checkerframework.checker.optional;

import com.sun.source.tree.*;
import com.sun.source.tree.Tree.Kind;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
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

    public OptionalVisitor(BaseTypeChecker checker) {
        super(checker);
        getMethod =
                TreeUtils.getMethod(
                        java.util.Optional.class.getName(),
                        "get",
                        0,
                        atypeFactory.getProcessingEnv());
        isPresentMethod =
                TreeUtils.getMethod(
                        java.util.Optional.class.getName(),
                        "isPresent",
                        0,
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

    /** e is a MethodInvocationTree */
    private ExpressionTree receiver(ExpressionTree e) {
        MethodInvocationTree invok = (MethodInvocationTree) e;
        ExpressionTree methodSelect = invok.getMethodSelect();
        ExpressionTree receiver = ((MemberSelectTree) methodSelect).getExpression();
        return TreeUtils.skipParens(receiver);
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
        // System.out.printf("visitConditionalExpression(%s)%n", node);
        handleTernaryIsPresentGet(node);
        return super.visitConditionalExpression(node, p);
    }

    /*
     * Pattern match for:  {@code VAR.isPresent() ? VAR.get().METHOD() : VALUE}
     *
     * <p>Prefer:  {@code VAR.map(METHOD).orElse(VALUE);}
     */
    public void handleTernaryIsPresentGet(ConditionalExpressionTree node) {

        // System.out.printf("handleTernaryIsPresentGet(%s)%n", node);

        ExpressionTree condExpr = TreeUtils.skipParens(node.getCondition());
        ExpressionTree trueExpr = TreeUtils.skipParens(node.getTrueExpression());
        ExpressionTree falseExpr = TreeUtils.skipParens(node.getFalseExpression());

        // System.out.printf("condExpr=%s, trueExpr=%s, falseExpr=%s%n", condExpr, trueExpr, falseExpr);

        if (!isCallToIsPresent(condExpr)) {
            return;
        }
        MethodInvocationTree isPresentInvok = (MethodInvocationTree) condExpr;
        ExpressionTree isPresent = isPresentInvok.getMethodSelect();
        // System.out.printf("isPresentInvok=%s, isPresent=%s%n", isPresentInvok, isPresent);
        if (!(isPresent instanceof MemberSelectTree)) {
            return;
        }
        ExpressionTree receiver =
                TreeUtils.skipParens(((MemberSelectTree) isPresent).getExpression());
        // System.out.printf("receiver=%s%n", receiver);
        if (!(trueExpr instanceof MethodInvocationTree)) {
            return;
        }
        MethodInvocationTree trueMethodInvok = (MethodInvocationTree) trueExpr;
        List<? extends ExpressionTree> trueArgs = trueMethodInvok.getArguments();
        // System.out.printf("trueMethodInvok=%s, trueArgs=%s%n", trueMethodInvok, trueArgs);
        if (!trueArgs.isEmpty()) {
            return;
        }
        MemberSelectTree trueMethod = (MemberSelectTree) trueMethodInvok.getMethodSelect();
        ExpressionTree trueReceiver = TreeUtils.skipParens(trueMethod.getExpression());
        // System.out.printf("trueMethod=%s, trueReceiver=%s%n", trueMethod, trueReceiver);
        if (!isCallToGet(trueReceiver)) {
            return;
        }
        ExpressionTree getReceiver = receiver(trueReceiver);
        // System.out.printf("getReceiver=%s%n", getReceiver);

        // What is a better way to do this than string comparison?
        if (receiver.toString().equals(getReceiver.toString())) {
            // System.out.printf("issuing warning");
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
        // System.out.printf("visitIf: %s%n", node);
        handleConditionalStatementIsPresentGet(node);
        return super.visitIf(node, p);
    }

    /*
     * Pattern match for: {@code if (VAR.isPresent()) { METHOD(VAR.get()); }}
     *
     * <p>Prefer:  {@code VAR.ifPresent(METHOD);}
     */
    public void handleConditionalStatementIsPresentGet(IfTree node) {

        ExpressionTree condExpr = TreeUtils.skipParens(node.getCondition());
        StatementTree thenStmt = TreeUtils.skipBlocks(node.getThenStatement());
        StatementTree elseStmt = TreeUtils.skipBlocks(node.getElseStatement());

        // System.out.printf("condExpr=%s (%s)%n", condExpr, condExpr.getKind());
        // System.out.printf("thenStmt=%s (%s)%n", condExpr, condExpr.getKind());
        // System.out.printf("elseStmt=%s (%s)%n", elseStmt, elseStmt != null ? elseStmt.getKind() : null);
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
        // System.out.printf("receiver=%s%n", receiver);
        if (thenStmt.getKind() != Kind.EXPRESSION_STATEMENT) {
            return;
        }
        ExpressionTree thenExpr = ((ExpressionStatementTree) thenStmt).getExpression();
        if (thenExpr.getKind() != Kind.METHOD_INVOCATION) {
            return;
        }
        MethodInvocationTree invok = (MethodInvocationTree) thenExpr;
        List<? extends ExpressionTree> args = invok.getArguments();
        // System.out.printf("invok=%s, args=%s%n", invok, args);
        if (args.size() != 1) {
            return;
        }
        ExpressionTree arg = TreeUtils.skipParens(args.get(0));
        // System.out.printf("arg=%s%n", arg);
        if (!isCallToGet(arg)) {
            return;
        }
        ExpressionTree getReceiver = receiver(arg);
        // System.out.printf("getReceiver=%s%n", getReceiver);
        if (!receiver.toString().equals(getReceiver.toString())) {
            return;
        }
        ExpressionTree method = invok.getMethodSelect();
        // System.out.printf("method=%s%n", method);

        // System.out.printf("issuing warning%n");
        String methodString = method.toString();
        int dotPos = methodString.lastIndexOf(".");
        if (dotPos != -1) {
            methodString =
                    methodString.substring(0, dotPos) + "::" + methodString.substring(dotPos + 1);
        }

        checker.report(Result.warning("prefer.ifpresent", receiver, methodString), node);
    }
}
