package org.checkerframework.checker.signedness;

import com.sun.source.tree.*;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * The SignednessVisitor enforces the Signedness Checker rules. These rules are described in detail
 * in the Checker Framework Manual.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
public class SignednessVisitor extends BaseTypeVisitor<SignednessAnnotatedTypeFactory> {

    public SignednessVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * @param node
     * @return true iff node is a relevant mask operation (& or |).
     */
    private boolean isMask(Tree node) {
        Kind kind = node.getKind();

        return kind == Kind.AND || kind == Kind.OR;
    }

    /**
     * @param expr
     * @return true iff expr is a literal.
     */
    private boolean isLiteral(ExpressionTree expr) {
        return expr instanceof LiteralTree;
    }

    /**
     * @param obj either an Integer or a Long
     * @return the long value of obj.
     */
    private long getLong(Object obj) {
        if (obj instanceof Integer) {
            Integer intObj = (Integer) obj;
            return intObj.longValue();
        } else {
            return (long) obj;
        }
    }

    /**
     * @param maskKind
     * @param shiftLit The LiteralTree whose value is s.
     * @param maskLit The LiteralTree whose value is m.
     * @return true iff the s most significant bits of m are 0 for AND, and 1 for OR.
     */
    private boolean isMaskedShift(Kind maskKind, LiteralTree shiftLit, LiteralTree maskLit) {
        long s = getLong(shiftLit.getValue());
        long m = getLong(maskLit.getValue());

        if (maskLit.getKind() != Kind.LONG_LITERAL) m <<= 32;

        m >>>= (64 - s);
        if (maskKind == Kind.AND) {
            return m == 0;
        } else if (maskKind == Kind.OR) {
            return m == (1 << s) - 1;
        } else return false;
    }

    /**
     * @param shiftOp
     * @return true iff the shift is masked such that the type of shift is irrelevant.
     */
    private boolean irrelevantShift(BinaryTree shiftOp) {
        TreePath ancestorPath = visitorState.getPath().getParentPath();
        Tree ancestor = ancestorPath.getLeaf();

        while (ancestor.getKind() == Kind.PARENTHESIZED) {
            ancestorPath = ancestorPath.getParentPath();
            ancestor = ancestorPath.getLeaf();
        }

        if (!isMask(ancestor)) return false;

        BinaryTree maskOp = (BinaryTree) ancestor;
        ExpressionTree shiftExpr = shiftOp.getRightOperand();
        ExpressionTree maskExpr =
                maskOp.getRightOperand() == shiftOp
                        ? maskOp.getLeftOperand()
                        : maskOp.getRightOperand();

        if (!isLiteral(shiftExpr) || !isLiteral(maskExpr)) return false;

        LiteralTree shiftLit = (LiteralTree) shiftExpr;
        LiteralTree maskLit = (LiteralTree) maskExpr;

        return isMaskedShift(maskOp.getKind(), shiftLit, maskLit);
    }

    /**
     * Enforces the following rules on binary operations involving Unsigned and Signed types:
     *
     * <ul>
     *   <li> Do not allow any Unsigned types in {@literal {/, %}} operations.
     *   <li> Do not allow signed right shift {@literal {>>}} on an Unsigned type.
     *   <li> Do not allow unsigned right shift {@literal {>>>}} on a Signed type.
     *   <li> Allow any left shift {@literal {<<}}.
     *   <li> Do not allow non-equality comparisons {@literal {<, <=, >, >=}} on Unsigned types.
     *   <li> Do not allow the mixing of Signed and Unsigned types.
     * </ul>
     */
    @Override
    public Void visitBinary(BinaryTree node, Void p) {

        ExpressionTree leftOp = node.getLeftOperand();
        ExpressionTree rightOp = node.getRightOperand();
        AnnotatedTypeMirror leftOpType = atypeFactory.getAnnotatedType(leftOp);
        AnnotatedTypeMirror rightOpType = atypeFactory.getAnnotatedType(rightOp);

        Kind kind = node.getKind();

        switch (kind) {
            case DIVIDE:
            case REMAINDER:
                if (leftOpType.hasAnnotation(Unsigned.class)) {
                    checker.report(Result.failure("operation.unsignedlhs", kind), leftOp);
                } else if (rightOpType.hasAnnotation(Unsigned.class)) {
                    checker.report(Result.failure("operation.unsignedrhs", kind), rightOp);
                }
                break;

            case RIGHT_SHIFT:
                if (leftOpType.hasAnnotation(Unsigned.class) && !irrelevantShift(node)) {
                    checker.report(Result.failure("shift.signed", kind), leftOp);
                }
                break;

            case UNSIGNED_RIGHT_SHIFT:
                if (leftOpType.hasAnnotation(Signed.class) && !irrelevantShift(node)) {
                    checker.report(Result.failure("shift.unsigned", kind), leftOp);
                }
                break;

            case LEFT_SHIFT:
                break;

            case GREATER_THAN:
            case GREATER_THAN_EQUAL:
            case LESS_THAN:
            case LESS_THAN_EQUAL:
                if (leftOpType.hasAnnotation(Unsigned.class)) {
                    checker.report(Result.failure("comparison.unsignedlhs"), leftOp);
                } else if (rightOpType.hasAnnotation(Unsigned.class)) {
                    checker.report(Result.failure("comparison.unsignedrhs"), rightOp);
                }
                break;

            case EQUAL_TO:
            case NOT_EQUAL_TO:
                if (leftOpType.hasAnnotation(Unsigned.class)
                        && rightOpType.hasAnnotation(Signed.class)) {
                    checker.report(Result.failure("comparison.mixed.unsignedlhs"), node);
                } else if (leftOpType.hasAnnotation(Signed.class)
                        && rightOpType.hasAnnotation(Unsigned.class)) {
                    checker.report(Result.failure("comparison.mixed.unsignedrhs"), node);
                }
                break;

            default:
                if (leftOpType.hasAnnotation(Unsigned.class)
                        && rightOpType.hasAnnotation(Signed.class)) {
                    checker.report(Result.failure("operation.mixed.unsignedlhs", kind), node);
                } else if (leftOpType.hasAnnotation(Signed.class)
                        && rightOpType.hasAnnotation(Unsigned.class)) {
                    checker.report(Result.failure("operation.mixed.unsignedrhs", kind), node);
                }
                break;
        }
        return super.visitBinary(node, p);
    }

    /** @return a string representation of kind, with trailing _ASSIGNMENT stripped off if any */
    private String kindWithOutAssignment(Kind kind) {
        String result = kind.toString();
        if (result.endsWith("_ASSIGNMENT")) {
            return result.substring(0, result.length() - "_ASSIGNMENT".length());
        } else {
            return result;
        }
    }

    /**
     * Enforces the following rules on compound assignments involving Unsigned and Signed types:
     *
     * <ul>
     *   <li> Do not allow any Unsigned types in {@literal {/=, %=}} assignments.
     *   <li> Do not allow signed right shift {@literal {>>=}} to assign to an Unsigned type.
     *   <li> Do not allow unsigned right shift {@literal {>>>=}} to assign to a Signed type.
     *   <li> Allow any left shift {@literal {<<=}} assignment.
     *   <li> Do not allow mixing of Signed and Unsigned types.
     * </ul>
     */
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {

        ExpressionTree var = node.getVariable();
        ExpressionTree expr = node.getExpression();
        AnnotatedTypeMirror varType = atypeFactory.getAnnotatedType(var);
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(expr);

        Kind kind = node.getKind();

        switch (kind) {
            case DIVIDE_ASSIGNMENT:
            case REMAINDER_ASSIGNMENT:
                if (varType.hasAnnotation(Unsigned.class)) {
                    checker.report(
                            Result.failure(
                                    "compound.assignment.unsigned.variable",
                                    kindWithOutAssignment(kind)),
                            var);
                } else if (exprType.hasAnnotation(Unsigned.class)) {
                    checker.report(
                            Result.failure(
                                    "compound.assignment.unsigned.expression",
                                    kindWithOutAssignment(kind)),
                            expr);
                }
                break;

            case RIGHT_SHIFT_ASSIGNMENT:
                if (varType.hasAnnotation(Unsigned.class)) {
                    checker.report(
                            Result.failure(
                                    "compound.assignment.shift.signed",
                                    kindWithOutAssignment(kind),
                                    "unsigned"),
                            var);
                }
                break;

            case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
                if (varType.hasAnnotation(Signed.class)) {
                    checker.report(
                            Result.failure(
                                    "compound.assignment.shift.unsigned",
                                    kindWithOutAssignment(kind),
                                    "signed"),
                            var);
                }
                break;

            case LEFT_SHIFT_ASSIGNMENT:
                break;

            default:
                if (varType.hasAnnotation(Unsigned.class) && exprType.hasAnnotation(Signed.class)) {
                    checker.report(
                            Result.failure(
                                    "compound.assignment.mixed.unsigned.variable",
                                    kindWithOutAssignment(kind)),
                            expr);
                } else if (varType.hasAnnotation(Signed.class)
                        && exprType.hasAnnotation(Unsigned.class)) {
                    checker.report(
                            Result.failure(
                                    "compound.assignment.mixed.unsigned.expression",
                                    kindWithOutAssignment(kind)),
                            expr);
                }
                break;
        }
        return super.visitCompoundAssignment(node, p);
    }
}
