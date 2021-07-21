package org.checkerframework.checker.lock;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.Tree.Kind;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.TypesUtils;

public class LockTreeAnnotator extends TreeAnnotator {

    public LockTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
    }

    @Override
    public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
        // For any binary operation whose LHS or RHS can be a non-boolean type, and whose resulting
        // type is necessarily boolean, the resulting annotation on the boolean type must be
        // @GuardedBy({}).

        // There is no need to enforce that the annotation on the result of &&, ||, etc.  is
        // @GuardedBy({}) since for such operators, both operands are of type @GuardedBy({}) boolean
        // to begin with.

        if (isBinaryComparisonOrInstanceOfOperator(node.getKind())
                || TypesUtils.isString(type.getUnderlyingType())) {
            // A boolean or String is always @GuardedBy({}). LockVisitor determines whether
            // the LHS and RHS of this operation can be legally dereferenced.
            type.replaceAnnotation(((LockAnnotatedTypeFactory) atypeFactory).GUARDEDBY);

            return null;
        }

        return super.visitBinary(node, type);
    }

    /** Indicates that the result of the operation is a boolean value. */
    private static boolean isBinaryComparisonOrInstanceOfOperator(Kind opKind) {
        switch (opKind) {
            case EQUAL_TO:
            case NOT_EQUAL_TO:
                // Technically, <=, <, > and >= are irrelevant for visitBinary, since currently
                // boxed primitives cannot be annotated with @GuardedBy(...), but they are left here
                // in case that rule changes.
            case LESS_THAN:
            case LESS_THAN_EQUAL:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL:
            case INSTANCE_OF:
                return true;
            default:
        }

        return false;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
        if (TypesUtils.isString(type.getUnderlyingType())) {
            type.replaceAnnotation(((LockAnnotatedTypeFactory) atypeFactory).GUARDEDBY);
        }

        return super.visitCompoundAssignment(node, type);
    }
}
