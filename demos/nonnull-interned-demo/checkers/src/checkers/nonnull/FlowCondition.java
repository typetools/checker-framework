package checkers.nonnull;

import checkers.quals.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

/**
 * Represents a conditional expression that performs a nonnull check.
 * Specifically, it consists of the operand that is being checked, and whether
 * that operand is nonnull or null within the scope of the check. 
 * <p>
 * {@link FlowCondition} abstracts away the type of conditional check (==, !=,
 * instanceof, etc.); since the Tree API exposes most conditions as {@link
 * ExpressionTree}s, a flow-sensitive analysis like {@link FlowVisitor} that
 * uses this class doesn't need to know about specific types of conditions.
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
class FlowCondition {
   
    /** The operand for the conditional check. */
    private final ExpressionTree operand;

    /** Whether the operand is nonnull within the scope of the check. */
    private final boolean nonnull;

    /**
     * A simple visitor that resolves the {@link FlowCondition} for different
     * tree types without ugly casts.
     */
    private static class Factory extends SimpleTreeVisitor<FlowCondition, Void> {

        @Override
        public @Nullable FlowCondition visitBinary(BinaryTree node, Void p) {
            @Nullable ExpressionTree operand = null;
            
            if (node.getKind() == Tree.Kind.CONDITIONAL_AND) {
                @Nullable FlowCondition left = visit(node.getLeftOperand(), null);
                @Nullable FlowCondition right = visit(node.getRightOperand(), null);
                if (left != null)
                    return left;
                else if (right != null)
                    return right;
            }
            
            // One of the operands must be a null literal.
            if (node.getLeftOperand().getKind() == Tree.Kind.NULL_LITERAL)
                operand = node.getRightOperand();
            else if (node.getRightOperand().getKind() == Tree.Kind.NULL_LITERAL)
                operand = node.getLeftOperand();
            else return null;

            // The operator must be == or !=.
            if (node.getKind() == Tree.Kind.NOT_EQUAL_TO)
                return new FlowCondition(operand, true);
            else if (node.getKind() == Tree.Kind.EQUAL_TO)
                return new FlowCondition(operand, false);
            else return null;
        }

        @Override
        public FlowCondition visitInstanceOf(InstanceOfTree node, Void p) {
            return new FlowCondition(node.getExpression(), true);
        }

        @Override
        public @Nullable FlowCondition visitParenthesized(ParenthesizedTree node, Void p) {
            // Reduce parens.
            return visit(node.getExpression(), p);
        }
    }

    /**
     * Creates a new, immutable FlowCondition.
     *
     * @param operand the operand being checked
     * @param isNonNull whether the operand is nonnull in the scope of the
     *                  check
     */
    private FlowCondition(ExpressionTree operand, boolean isNonNull) {
        this.operand = operand;
        this.nonnull = isNonNull;
    }

    /** 
     * @return true if the condition's operand is nonnull in the scope of the
     *         condition
     */
    public boolean isNonNull() { 
        return nonnull; 
    } 

    /**
     * @return the operand for the condition
     */
    public ExpressionTree getOperand() { 
        return operand;
    }

    /**
     * Creates a FlowCondition from the tree for a conditional nonnull check.
     *
     * @param tree the tree representing a conditional nonnull check
     * @return the {@link FlowCondition} for the condition, or null if the
     *         given tree isn't a conditional nonnull check
     */
    public static @Nullable FlowCondition create(ExpressionTree tree) {
        if (tree == null)
            return null;
        Factory f = new Factory();
        return f.visit(tree, null);
    }

    @Override
    public String toString() {
        return String.format("[%s (%s)]", operand, 
                (nonnull ? "nonnull" : "null"));
    }
}

