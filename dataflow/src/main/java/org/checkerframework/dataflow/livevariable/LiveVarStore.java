package org.checkerframework.dataflow.livevariable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;
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
import org.plumelib.util.ArraySet;

/** A live variable store contains a set of live variables represented by nodes. */
public class LiveVarStore implements Store<LiveVarStore> {

  /** The set of live variables in this store. */
  private final Set<LiveVarNode> liveVarNodeSet;

  /** Create a new LiveVarStore. */
  public LiveVarStore() {
    liveVarNodeSet = new LinkedHashSet<>();
  }

  /**
   * Create a new LiveVarStore.
   *
   * @param liveVarNodeSet the set of live variable nodes. The parameter is captured and the caller
   *     should not retain an alias.
   */
  public LiveVarStore(Set<LiveVarNode> liveVarNodeSet) {
    this.liveVarNodeSet = liveVarNodeSet;
  }

  /**
   * Add the information of a live variable into the live variable set.
   *
   * @param variable a live variable
   */
  public void putLiveVar(LiveVarNode variable) {
    liveVarNodeSet.add(variable);
  }

  /**
   * Remove the information of a live variable from the live variable set.
   *
   * @param variable a live variable
   */
  public void killLiveVar(LiveVarNode variable) {
    liveVarNodeSet.remove(variable);
  }

  /**
   * Add the information of live variables in an expression to the live variable set.
   *
   * @param expression a node
   */
  public void addUseInExpression(Node expression) {
    // TODO Do we need a AbstractNodeScanner to do the following job?
    if (expression instanceof LocalVariableNode || expression instanceof FieldAccessNode) {
      LiveVarNode liveVarValue = new LiveVarNode(expression);
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
    return other.liveVarNodeSet.equals(this.liveVarNodeSet);
  }

  @Override
  public int hashCode() {
    return this.liveVarNodeSet.hashCode();
  }

  @Override
  public LiveVarStore copy() {
    return new LiveVarStore(new HashSet<>(liveVarNodeSet));
  }

  @Override
  public LiveVarStore leastUpperBound(LiveVarStore other) {
    Set<LiveVarNode> liveVarNodeSetLub =
        ArraySet.newArraySetOrLinkedHashSet(
            this.liveVarNodeSet.size() + other.liveVarNodeSet.size());
    liveVarNodeSetLub.addAll(this.liveVarNodeSet);
    liveVarNodeSetLub.addAll(other.liveVarNodeSet);
    return new LiveVarStore(liveVarNodeSetLub);
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
    if (liveVarNodeSet.isEmpty()) {
      return viz.visualizeStoreKeyVal(key, "none");
    }
    StringJoiner sjStoreVal = new StringJoiner(", ");
    for (LiveVarNode liveVar : liveVarNodeSet) {
      sjStoreVal.add(liveVar.toString());
    }
    return viz.visualizeStoreKeyVal(key, sjStoreVal.toString());
  }

  @Override
  public String toString() {
    return liveVarNodeSet.toString();
  }
}
