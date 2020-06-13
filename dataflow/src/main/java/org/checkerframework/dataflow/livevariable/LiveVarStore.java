package org.checkerframework.dataflow.livevariable;

import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.dataflow.cfg.node.BinaryOperationNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.InstanceOfNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.cfg.node.UnaryOperationNode;
import org.checkerframework.javacutil.BugInCF;

/** A live variable store contains a set of live variables represented by nodes. */
public class LiveVarStore implements Store<LiveVarStore> {

    /** A set of live variable abstract values. */
    private final Set<LiveVar> liveVarSet;

    /** Create a new LiveVarStore. */
    public LiveVarStore() {
        liveVarSet = new HashSet<>();
    }

    /**
     * Create a new LiveVarStore.
     *
     * @param liveVarSet a set of live variable abstract values
     */
    public LiveVarStore(Set<LiveVar> liveVarSet) {
        this.liveVarSet = liveVarSet;
    }

    /**
     * Add the information of a live variable into {@link #liveVarSet}.
     *
     * @param variable a live variable
     */
    public void putLiveVar(LiveVar variable) {
        liveVarSet.add(variable);
    }

    /**
     * Remove the information of a live variable from {@link #liveVarSet}.
     *
     * @param variable a live variable
     */
    public void killLiveVar(LiveVar variable) {
        liveVarSet.remove(variable);
    }

    /**
     * Add the information of live variables in an expression to {@link #liveVarSet}.
     *
     * @param expression a node
     */
    public void addUseInExpression(Node expression) {
        // TODO Do we need a AbstractNodeScanner to do the following job?
        if (expression instanceof LocalVariableNode || expression instanceof FieldAccessNode) {
            LiveVar liveVar = new LiveVar(expression);
            putLiveVar(liveVar);
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
        return other.liveVarSet.equals(this.liveVarSet);
    }

    @Override
    public int hashCode() {
        return this.liveVarSet.hashCode();
    }

    @Override
    public LiveVarStore copy() {
        Set<LiveVar> liveVarSetCopy = new HashSet<>(liveVarSet);
        return new LiveVarStore(liveVarSetCopy);
    }

    @Override
    public LiveVarStore leastUpperBound(LiveVarStore other) {
        Set<LiveVar> liveVarSetLub = new HashSet<>();
        liveVarSetLub.addAll(this.liveVarSet);
        liveVarSetLub.addAll(other.liveVarSet);
        return new LiveVarStore(liveVarSetLub);
    }

    /** It should not be called since it is not used by the backward analysis. */
    @Override
    public LiveVarStore widenedUpperBound(LiveVarStore previous) {
        throw new BugInCF("wub of LiveVarStore get called!");
    }

    @Override
    public boolean canAlias(Receiver a, Receiver b) {
        return true;
    }

    @Override
    public String visualize(CFGVisualizer<?, LiveVarStore, ?> viz) {
        if (liveVarSet.isEmpty()) {
            return "No live variables.";
        }
        StringBuilder sbStoreVal = new StringBuilder();
        for (LiveVar liveVar : liveVarSet) {
            sbStoreVal.append(viz.visualizeStoreKeyVal("live variable", liveVar.liveVariable));
        }
        return sbStoreVal.toString();
    }

    @Override
    public String toString() {
        return liveVarSet.toString();
    }
}
