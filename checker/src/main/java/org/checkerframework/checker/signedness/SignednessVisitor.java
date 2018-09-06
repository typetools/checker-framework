package org.checkerframework.checker.signedness;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

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

    /** @return type of a primitive cast, or null if not primitive */
    private PrimitiveTypeTree primitiveTypeCast(Tree node) {
        if (node.getKind() != Kind.TYPE_CAST) {
            return null;
        }

        TypeCastTree cast = (TypeCastTree) node;
        Tree castType = cast.getType();
        // All types should be wrapped in some annotation
        if (castType.getKind() != Kind.ANNOTATED_TYPE) {
            return null;
        }

        AnnotatedTypeTree annotatedType = (AnnotatedTypeTree) castType;
        ExpressionTree underlyingType = annotatedType.getUnderlyingType();

        if (underlyingType.getKind() != Kind.PRIMITIVE_TYPE) {
            return null;
        }

        return (PrimitiveTypeTree) underlyingType;
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
     * the shiftAmount most significant bits of expr. This is if the shiftAmount most significant
     * bits of mask are 0 for AND, and 1 for OR. For example, assuming that shiftAmount is 4, the
     * following is true about AND and OR masks:
     *
     * <p>{@code expr & 0x0... == 0x0... ;}
     *
     * <p>{@code expr | 0xF... == 0xF... ;}
     *
     * @param maskKind the kind of mask (AND or OR)
     * @param shiftAmountLit the LiteralTree whose value is shiftAmount
     * @param maskLit the LiteralTree whose value is mask
     * @return true iff the shiftAmount most significant bits of mask are 0 for AND, and 1 for OR
     */
    private boolean maskIgnoresMSB(Kind maskKind, LiteralTree shiftAmountLit, LiteralTree maskLit) {
        long shiftAmount = getLong(shiftAmountLit.getValue());
        long mask = getLong(maskLit.getValue());

        // Shift of zero is a nop
        if (shiftAmount == 0) {
            return true;
        }

        // Shift the shiftAmount most significant bits to become the shiftAmount least significant
        // bits, zeroing out the rest.
        if (maskLit.getKind() != Kind.LONG_LITERAL) {
            mask <<= 32;
        }
        mask >>>= (64 - shiftAmount);

        if (maskKind == Kind.AND) {
            // Check that the shiftAmount most significant bits of the mask were 0.
            return mask == 0;
        } else if (maskKind == Kind.OR) {
            // Check that the shiftAmount most significant bits of the mask were 1.
            return mask == (1 << shiftAmount) - 1;
        } else {
            throw new BugInCF("Invalid Masking Operation");
        }
    }

    /**
     * Given a casted right shift of the form {@code (type) (baseExpr >> shiftAmount)} or {@code
     * (type) (baseExpr >>> shiftAmount)}, return true iff the expression's value is the same
     * regardless of the type of right shift (signed or unsigned). This is true if the cast ignores
     * the shiftAmount most significant bits of the shift result -- that is, if the cast ignores all
     * the new bits that the right shift introduced on the left.
     *
     * <p>For example, the function returns true for
     *
     * <pre>{@code (short) (myInt >> 16)}</pre>
     *
     * and for
     *
     * <pre>{@code (short) (myInt >>> 16)}</pre>
     *
     * because these two expressions are guaranteed to have the same result.
     *
     * @param shiftTypeKind the kind of the type of the shift literal (BYTE, CHAR, SHORT, INT, or
     *     LONG)
     * @param castTypeKind the kind of the cast target type (BYTE, CHAR, SHORT, INT, or LONG)
     * @param shiftAmountLit the LiteralTree whose value is shiftAmount
     * @return true iff introduced bits are discarded
     */
    private boolean castIgnoresMSB(
            TypeKind shiftTypeKind, TypeKind castTypeKind, LiteralTree shiftAmountLit) {
        // Determine number of bits in the shift type, note shifts upcast to int.
        // Also determine the shift amount as it is dependent on the shift type.
        long shiftBits;
        long shiftAmount;
        switch (shiftTypeKind) {
            case INT:
                shiftBits = 32;
                // When the LHS of the shift is an int, the 5 lower order bits of the RHS are used.
                shiftAmount = 0x1F & getLong(shiftAmountLit.getValue());
                break;
            case LONG:
                shiftBits = 64;
                // When the LHS of the shift is a long, the 6 lower order bits of the RHS are used.
                shiftAmount = 0x3F & getLong(shiftAmountLit.getValue());
                break;
            default:
                throw new BugInCF("Invalid shift type");
        }

        // Determine number of bits in the cast type
        long castBits;
        switch (castTypeKind) {
            case BYTE:
                castBits = 8;
                break;
            case CHAR:
                castBits = 8;
                break;
            case SHORT:
                castBits = 16;
                break;
            case INT:
                castBits = 32;
                break;
            case LONG:
                castBits = 64;
                break;
            default:
                throw new BugInCF("Invalid cast target");
        }

        long bitsDiscarded = shiftBits - castBits;

        return shiftAmount <= bitsDiscarded || shiftAmount == 0;
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
        ExpressionTree shiftAmountExpr = shiftExpr.getRightOperand();

        // Determine which child of maskExpr leads to shiftExpr. The other one is the mask.
        ExpressionTree mask =
                maskExpr.getRightOperand() == enclosingChild
                        ? maskExpr.getLeftOperand()
                        : maskExpr.getRightOperand();

        // Strip away the parentheses from the mask if any exist
        mask = TreeUtils.skipParens(mask);

        if (!isLiteral(shiftAmountExpr) || !isLiteral(mask)) {
            return false;
        }

        LiteralTree shiftLit = (LiteralTree) shiftAmountExpr;
        LiteralTree maskLit = (LiteralTree) mask;

        return maskIgnoresMSB(maskExpr.getKind(), shiftLit, maskLit);
    }

    /**
     * Determines if a right shift operation, {@code >>} or {@code >>>}, is type casted such that
     * the cast renders the shift signedness ({@code >>} vs {@code >>>}) irrelevent by discarding
     * the bits duplicated into the shift result. For example, the following pair of right shifts on
     * {@code short s} both produce the same results under any input, because of type casting:
     *
     * <p>{@code (byte)(s >> 8) == (byte)(b >>> 8);}
     *
     * @param shiftExpr a right shift expression: {@code expr1 >> expr2} or {@code expr1 >>> expr2}
     * @return true iff the right shift is type casted such that a signed or unsigned right shift
     *     has the same effect
     */
    private boolean isCastedShift(BinaryTree shiftExpr) {
        // enclosing is the operation or statement that immediately contains shiftExpr
        Tree enclosing;
        {
            TreePath parentPath = visitorState.getPath().getParentPath();
            enclosing = parentPath.getLeaf();
            // Strip away all parentheses from the shift operation
            while (enclosing.getKind() == Kind.PARENTHESIZED) {
                parentPath = parentPath.getParentPath();
                enclosing = parentPath.getLeaf();
            }
        }

        PrimitiveTypeTree castPrimitiveType = primitiveTypeCast(enclosing);
        if (castPrimitiveType == null) {
            return false;
        }
        TypeKind castTypeKind = castPrimitiveType.getPrimitiveTypeKind();

        // Determine the type of the shift result
        TypeKind shiftTypeKind =
                atypeFactory.getAnnotatedType(shiftExpr).getUnderlyingType().getKind();

        // Determine shift literal
        ExpressionTree shiftAmountExpr = shiftExpr.getRightOperand();
        if (!isLiteral(shiftAmountExpr)) {
            return false;
        }
        LiteralTree shiftLit = (LiteralTree) shiftAmountExpr;

        return castIgnoresMSB(shiftTypeKind, castTypeKind, shiftLit);
    }

    /**
     * Enforces the following rules on binary operations involving Unsigned and Signed types:
     *
     * <ul>
     *   <li>Do not allow any Unsigned types in {@literal {/, %}} operations.
     *   <li>Do not allow signed right shift {@literal {>>}} on an Unsigned type.
     *   <li>Do not allow unsigned right shift {@literal {>>>}} on a Signed type.
     *   <li>Allow any left shift {@literal {<<}}.
     *   <li>Do not allow non-equality comparisons {@literal {<, <=, >, >=}} on Unsigned types.
     *   <li>Do not allow the mixing of Signed and Unsigned types.
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
                if (leftOpType.hasAnnotation(Unsigned.class)
                        && !isMaskedShift(node)
                        && !isCastedShift(node)) {
                    checker.report(Result.failure("shift.signed", kind), leftOp);
                }
                break;

            case UNSIGNED_RIGHT_SHIFT:
                if (leftOpType.hasAnnotation(Signed.class)
                        && !isMaskedShift(node)
                        && !isCastedShift(node)) {
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
     *   <li>Do not allow any Unsigned types in {@literal {/=, %=}} assignments.
     *   <li>Do not allow signed right shift {@literal {>>=}} to assign to an Unsigned type.
     *   <li>Do not allow unsigned right shift {@literal {>>>=}} to assign to a Signed type.
     *   <li>Allow any left shift {@literal {<<=}} assignment.
     *   <li>Do not allow mixing of Signed and Unsigned types.
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
