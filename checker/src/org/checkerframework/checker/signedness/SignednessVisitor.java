package org.checkerframework.checker.signedness;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
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

    /** @return true iff node is a mask operation (&amp; or |) */
    private boolean isMask(Tree node) {
        Kind kind = node.getKind();

        return kind == Kind.AND || kind == Kind.OR;
    }

    /** @return true iff expr is a literal */
    private boolean isLiteral(ExpressionTree expr) {
        return expr instanceof LiteralTree;
    }

    /**
     * @param obj either an Integer or a Long
     * @return the long value of obj
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
     * Given a masking operation of the form {@code expr & maskLit} or {@code expr | maskLit},
     * return true iff the masking operation results in the same output regardless of the value of
     * the numBits most significant bits of expr. This is if the numBits most significant bits of
     * mask are 0 for AND, and 1 for OR. For example, assuming that numBits is 4, the following is
     * true about AND and OR masks:
     *
     * <p>{@code expr & 0x0... == 0x0... ;}
     *
     * <p>{@code expr | 0xF... == 0xF... ;}
     *
     * @param maskKind the kind of mask (AND or OR)
     * @param numBitsLit the LiteralTree whose value is numBits
     * @param maskLit the LiteralTree whose value is mask
     * @return true iff the numBits most significant bits of mask are 0 for AND, and 1 for OR
     */
    private boolean maskIgnoresMSB(Kind maskKind, LiteralTree numBitsLit, LiteralTree maskLit) {
        long numBits = getLong(numBitsLit.getValue());
        long mask = getLong(maskLit.getValue());

        // Shift the numBits most significant bits to become the numBits least significant bits, zeroing out the rest.
        if (maskLit.getKind() != Kind.LONG_LITERAL) {
            mask <<= 32;
        }
        mask >>>= (64 - numBits);

        if (maskKind == Kind.AND) {
            // Check that the numBits most significant bits of the mask were 0.
            return mask == 0;
        } else if (maskKind == Kind.OR) {
            // Check that the numBits most significant bits of the mask were 1.
            return mask == (1 << numBits) - 1;
        } else {
            // This shouldn't be possible.
            throw new RuntimeException("Invalid Masking Operation");
        }
    }

    /**
     * Determines if a right shift operation, {@code >>} or {@code >>>}, is masked with a masking
     * operation of the form {@code shiftExpr & maskLit} or {@code shiftExpr | maskLit} such that
     * the mask renders the shift signedness ({@code >>} vs {@code >>>}) irrelevent by destroying
     * the bits duplicated into the shift result. For example, the following pairs of right shifts
     * on {@code byte b} both produce the same results under any input, because of their masks:
     *
     * <p>{@code (b >> 4) & 0x0F == (b >>> 4) & 0x0F;}
     *
     * <p>{@code (b >> 4) | 0xF0 == (b >>> 4) | 0xF0;}
     *
     * @param shiftExpr a right shift expression: {@code expr1 >> expr2} or {@code expr1 >>> expr2}
     * @return true iff the right shift is masked such that a signed or unsigned right shift has the
     *     same effect
     */
    private boolean isMaskedShift(BinaryTree shiftExpr) {
        // enclosing is the operation or statement that immediately contains shiftExpr
        Tree enclosing;
        // enclosingChild is the top node in the chain of nodes from shiftExpr to parent
        Tree enclosingChild;
        {
            TreePath parentPath = visitorState.getPath().getParentPath();
            enclosing = parentPath.getLeaf();
            enclosingChild = enclosing;
            // Strip away all parentheses from the shift operation
            while (enclosing.getKind() == Kind.PARENTHESIZED) {
                parentPath = parentPath.getParentPath();
                enclosingChild = enclosing;
                enclosing = parentPath.getLeaf();
            }
        }

        if (!isMask(enclosing)) {
            return false;
        }

        BinaryTree maskExpr = (BinaryTree) enclosing;
        ExpressionTree shift = shiftExpr.getRightOperand();

        // Determine which child of maskExpr leads to shiftExpr. The other one is the mask.
        ExpressionTree mask =
                maskExpr.getRightOperand() == enclosingChild
                        ? maskExpr.getLeftOperand()
                        : maskExpr.getRightOperand();

        // Strip away the parentheses from the mask if any exist
        while (mask.getKind() == Kind.PARENTHESIZED)
            mask = ((ParenthesizedTree) mask).getExpression();

        if (!isLiteral(shift) || !isLiteral(mask)) {
            return false;
        }

        LiteralTree shiftLit = (LiteralTree) shift;
        LiteralTree maskLit = (LiteralTree) mask;

        return maskIgnoresMSB(maskExpr.getKind(), shiftLit, maskLit);
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
                if (leftOpType.hasAnnotation(Unsigned.class) && !isMaskedShift(node)) {
                    checker.report(Result.failure("shift.signed", kind), leftOp);
                }
                break;

            case UNSIGNED_RIGHT_SHIFT:
                if (leftOpType.hasAnnotation(Signed.class) && !isMaskedShift(node)) {
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
