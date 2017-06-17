package org.checkerframework.checker.optional;

import com.sun.source.tree.*;
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
        return receiver;
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
        // Pattern match for:  VAR.isPresent() ? VAR.get().METHOD() : VALUE
        // Prefer:  VAR.map(METHOD).orElse(VALUE);

        ExpressionTree condExpr = node.getCondition();
        ExpressionTree trueExpr = node.getTrueExpression();
        ExpressionTree falseExpr = node.getFalseExpression();

        if (isCallToIsPresent(condExpr)) {
            MethodInvocationTree isPresentInvok = (MethodInvocationTree) condExpr;
            ExpressionTree isPresent = isPresentInvok.getMethodSelect();
            System.out.printf("isPresentInvok=%s, isPresent=%s%n", isPresentInvok, isPresent);
            if (isPresent instanceof MemberSelectTree) {
                ExpressionTree receiver = ((MemberSelectTree) isPresent).getExpression();
                System.out.printf("receiver=%s%n", receiver);
                if (trueExpr instanceof MethodInvocationTree) {
                    MethodInvocationTree trueMethodInvok = (MethodInvocationTree) trueExpr;
                    List<? extends ExpressionTree> trueArgs = trueMethodInvok.getArguments();
                    System.out.printf(
                            "trueMethodInvok=%s, trueArgs=%s%n", trueMethodInvok, trueArgs);
                    if (trueArgs.isEmpty()) {
                        MemberSelectTree trueMethod =
                                (MemberSelectTree) trueMethodInvok.getMethodSelect();
                        ExpressionTree trueReceiver = trueMethod.getExpression();
                        System.out.printf(
                                "trueMethod=%s, trueReceiver=%s%n", trueMethod, trueReceiver);
                        if (isCallToGet(trueReceiver)) {
                            ExpressionTree getReceiver = receiver(trueReceiver);
                            System.out.printf("getReceiver=%s%n", getReceiver);
                            // What is a better way to do this than string comparison?
                            if (receiver.toString().equals(getReceiver.toString())) {
                                System.out.printf("issuing warning");
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
                    }
                }
            }
        }
        return super.visitConditionalExpression(node, p);
    }
}
