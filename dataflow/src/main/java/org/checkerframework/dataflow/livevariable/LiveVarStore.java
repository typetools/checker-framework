package org.checkerframework.dataflow.livevariable;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.BinaryOperationNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.InstanceOfNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.cfg.node.UnaryOperationNode;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.javacutil.BugInCF;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;

/** A live variable store contains a set of live variables represented by nodes. */
public class LiveVarStore implements Store<LiveVarStore> {

    /** A set of live variable abstract values. */
    private final Set<LiveVarValue> liveVarValueSet;

    /** Create a new LiveVarStore. */
    public LiveVarStore() {
        liveVarValueSet = new LinkedHashSet<>();
    }

    /**
     * Create a new LiveVarStore.
     *
     * @param liveVarValueSet a set of live variable abstract values
     */
    public LiveVarStore(Set<LiveVarValue> liveVarValueSet) {
        this.liveVarValueSet = liveVarValueSet;
    }

    /**
     * Add the information of a live variable into the live variable set.
     *
     * @param variable a live variable
     */
    public void putLiveVar(LiveVarValue variable) {
        liveVarValueSet.add(variable);
    }

    /**
     * Remove the information of a live variable from the live variable set.
     *
     * @param variable a live variable
     */
    public void killLiveVar(LiveVarValue variable) {
        liveVarValueSet.remove(variable);
    }

    /**
     * Add the information of live variables in an expression to the live variable set.
     *
     * @param expression a node
     */
    public void addUseInExpression(Node expression) {
        // TODO Do we need a AbstractNodeScanner to do the following job?
        if (expression instanceof LocalVariableNode || expression instanceof FieldAccessNode) {
            LiveVarValue liveVarValue = new LiveVarValue(expression);
            putLiveVar(liveVarValue);
        } else if (expression instanceof UnaryOperationNode) {
            UnaryOperationNode unaryNode = (UnaryOperationNode) expression;
            addUseInExpression(unaryNode.getOperand());
        } else if (expression instanceof TernaryExpressionNode) {
            TernaryExpressionNode ternaryNode = (TernaryExpressionNode) expression;
            addUseInExpression(ternaryNode.getConditionOperand());
            addUseInExpression(ternaryNode.getThenOperand());
            addUseInExpression(ternaryNode.getElseOperand());
        } else if (expression instanceof TypeCastNode) {
            TypeCastNode typeCastNode = (TypeCastNode) expression;
            addUseInExpression(typeCastNode.getOperand());
        } else if (expression instanceof InstanceOfNode) {
            InstanceOfNode instanceOfNode = (InstanceOfNode) expression;
            addUseInExpression(instanceOfNode.getOperand());
        } else if (expression instanceof BinaryOperationNode) {
            BinaryOperationNode binaryNode = (BinaryOperationNode) expression;
            addUseInExpression(binaryNode.getLeftOperand());
            addUseInExpression(binaryNode.getRightOperand());
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof LiveVarStore)) {
            return false;
        }
        LiveVarStore other = (LiveVarStore) obj;
        return other.liveVarValueSet.equals(this.liveVarValueSet);
    }

    @Override
    public int hashCode() {
        return this.liveVarValueSet.hashCode();
    }

    @Override
    public LiveVarStore copy() {
        return new LiveVarStore(new HashSet<>(liveVarValueSet));
    }

    @Override
    public LiveVarStore leastUpperBound(LiveVarStore other) {
        Set<LiveVarValue> liveVarValueSetLub =
                new HashSet<>(this.liveVarValueSet.size() + other.liveVarValueSet.size());
        liveVarValueSetLub.addAll(this.liveVarValueSet);
        liveVarValueSetLub.addAll(other.liveVarValueSet);
        return new LiveVarStore(liveVarValueSetLub);
    }

    /** It should not be called since it is not used by the backward analysis. */
    @Override
    public LiveVarStore widenedUpperBound(LiveVarStore previous) {
        throw new BugInCF("wub of LiveVarStore get called!");
    }

    @Override
    public boolean canAlias(JavaExpression a, JavaExpression b) {
        return true;
    }

    @Override
    public String visualize(CFGVisualizer<?, LiveVarStore, ?> viz) {
        String key = "live variables";
        if (liveVarValueSet.isEmpty()) {
            return viz.visualizeStoreKeyVal(key, "none");
        }
        StringJoiner sjStoreVal = new StringJoiner(", ");
        for (LiveVarValue liveVarValue : liveVarValueSet) {
            sjStoreVal.add(liveVarValue.toString());
        }
        return viz.visualizeStoreKeyVal(key, sjStoreVal.toString());
    }

    @Override
    public String toString() {
        return liveVarValueSet.toString();
    }
}
