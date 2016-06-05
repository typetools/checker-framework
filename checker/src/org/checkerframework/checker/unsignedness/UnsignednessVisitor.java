package org.checkerframework.checker.unsignedness;

import org.checkerframework.checker.unsignedness.qual.Signed;
import org.checkerframework.checker.unsignedness.qual.Unsigned;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;

/**
 * The UnsignednessVisitor enforces the Unsignedness Checker rules. These rules
 * are described in detail in the Checker Framework Manual.
 *
 * @checker_framework.manual #unsignedness-checker Unsignedness Checker
 */
public class UnsignednessVisitor extends BaseTypeVisitor<UnsignednessAnnotatedTypeFactory> {

    public UnsignednessVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * Enforces the following rules on binary operations involving Unsigned and
     * Signed types:
     * <ul>
     *      <li> Do not allow any @Unsigned types in the {/, %} operations. </li>
     *      <li> Do not allow the signed right shift {>>} of an @Unsigned type. </li>
     *      <li> Do not allow the unsigned right shift {>>>} of a @Signed type. </li>
     *      <li> Do not allow non-equality comparisons {<, <=, >, >=} on @Unsigned types. </li>
     *      <li> Allow any left shift {<<}. </li>
     *      <li> Do not allow the mixing of @Signed and @Unsigned types. </li>
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
                checker.report(Result.failure("binary.operation.type.incompatible.unsignedlhs",
                                              kind), node);
            } else if (rightOpType.hasAnnotation(Unsigned.class)) {
                checker.report(Result.failure("binary.operation.type.incompatible.unsignedrhs",
                                              kind), node);
            }
            break;

        case RIGHT_SHIFT:
            if (leftOpType.hasAnnotation(Unsigned.class)) {
                checker.report(Result.failure("binary.operation.shift.signed.type.incompatible",
                                              kind), node);
            }
            break;

        case UNSIGNED_RIGHT_SHIFT:
            if (leftOpType.hasAnnotation(Signed.class)) {
                checker.report(Result.failure("binary.operation.shift.unsigned.type.incompatible",
                                              kind), node);
            }
            break;

        case LEFT_SHIFT:
            break;

        case GREATER_THAN:
        case GREATER_THAN_EQUAL:
        case LESS_THAN:
        case LESS_THAN_EQUAL:
            if (leftOpType.hasAnnotation(Unsigned.class)) {
                checker.report(Result.failure("binary.comparison.type.incompatible.unsignedlhs",
                                              kind), node);
            } else if (rightOpType.hasAnnotation(Unsigned.class)) {
                checker.report(Result.failure("binary.comparison.type.incompatible.unsignedrhs",
                                              kind), node);
            }
            break;

        case EQUAL_TO:
        case NOT_EQUAL_TO:
            if (leftOpType.hasAnnotation(Unsigned.class) && rightOpType.hasAnnotation(Signed.class)) {
                checker.report(Result.failure("binary.comparison.type.incompatible.mixed.unsignedlhs",
                                              kind), node);
            } else if (leftOpType.hasAnnotation(Signed.class) && rightOpType.hasAnnotation(Unsigned.class)) {
                checker.report(Result.failure("binary.comparison.type.incompatible.mixed.unsignedrhs",
                                              kind), node);
            }
            break;

        default:
            if (leftOpType.hasAnnotation(Unsigned.class) && rightOpType.hasAnnotation(Signed.class)) {
                checker.report(Result.failure("binary.operation.type.incompatible.mixed.unsignedlhs",
                                              kind), node);
            } else if (leftOpType.hasAnnotation(Signed.class) && rightOpType.hasAnnotation(Unsigned.class)) {
                checker.report(Result.failure("binary.operation.type.incompatible.mixed.unsignedrhs",
                                              kind), node);
            }
            break;
        }
        return super.visitBinary(node, p);
    }

    /**
     * Enforces the following rules on compound assignments involving Unsigned and
     * Signed types:
     * <ul>
     *      <li> Do not allow any @Unsigned types in the {/=, %=} assignments. </li>
     *      <li> Do not allow the signed right shift {>>=} to assign to an @Unsigned type. </li>
     *      <li> Do not allow the unsigned right shift {>>>=} to assign to a @Signed type. </li>
     *      <li> Allow any left shift {<<=} assignment. </li>
     *      <li> Do not allow the mixing of @Signed and @Unsigned types. </li>
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
                checker.report(Result.failure("compound.assignment.type.incompatible.unsigned.variable",
                                              kind), node);
            } else if (exprType.hasAnnotation(Unsigned.class)) {
                checker.report(Result.failure("compound.assignment.type.incompatible.unsigned.expression",
                                              kind), node);
            }
            break;

        case RIGHT_SHIFT_ASSIGNMENT:
            if (varType.hasAnnotation(Unsigned.class)) {
                checker.report(Result.failure("compound.assignment.shift.signed.type.incompatible",
                                              kind, "unsigned"), node);
            }
            break;

        case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
            if (varType.hasAnnotation(Signed.class)) {
                checker.report(Result.failure("compound.assignment.shift.unsigned.type.incompatible",
                                              kind, "signed"), node);
            }
            break;

        case LEFT_SHIFT_ASSIGNMENT:
            break;
        default:
            if (varType.hasAnnotation(Unsigned.class) && exprType.hasAnnotation(Signed.class)) {
                checker.report(Result.failure("compound.assignment.type.incompatible.mixed.unsigned.variable",
                                              kind), node);
            } else if (varType.hasAnnotation(Signed.class) && exprType.hasAnnotation(Unsigned.class)) {
                checker.report(Result.failure("compound.assignment.type.incompatible.mixed.unsigned.expression",
                                              kind), node);
            }
            break;
        }
        return super.visitCompoundAssignment(node, p);
    }
}
