package org.checkerframework.dataflow.livevariable;

import java.util.List;
import org.checkerframework.dataflow.analysis.BackwardTransferFunction;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.AbstractNodeVisitor;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.qual.SideEffectFree;

/** A live variable transfer function. */
public class LiveVarTransfer
    extends AbstractNodeVisitor<
        TransferResult<UnusedAbstractValue, LiveVarStore>,
        TransferInput<UnusedAbstractValue, LiveVarStore>>
    implements BackwardTransferFunction<UnusedAbstractValue, LiveVarStore> {

  /** Creates a new LiveVarTransfer. */
  public LiveVarTransfer() {}

  @Override
  @SideEffectFree
  public LiveVarStore initialNormalExitStore(
      UnderlyingAST underlyingAST, List<ReturnNode> returnNodes) {
    return new LiveVarStore();
  }

  @Override
  public LiveVarStore initialExceptionalExitStore(UnderlyingAST underlyingAST) {
    return new LiveVarStore();
  }

  @Override
  public RegularTransferResult<UnusedAbstractValue, LiveVarStore> visitNode(
      Node n, TransferInput<UnusedAbstractValue, LiveVarStore> p) {
    return new RegularTransferResult<>(null, p.getRegularStore());
  }

  @Override
  public RegularTransferResult<UnusedAbstractValue, LiveVarStore> visitAssignment(
      AssignmentNode n, TransferInput<UnusedAbstractValue, LiveVarStore> p) {
    RegularTransferResult<UnusedAbstractValue, LiveVarStore> transferResult =
        (RegularTransferResult<UnusedAbstractValue, LiveVarStore>) super.visitAssignment(n, p);
    processLiveVarInAssignment(n.getTarget(), n.getExpression(), transferResult.getRegularStore());
    return transferResult;
  }

  @Override
  public RegularTransferResult<UnusedAbstractValue, LiveVarStore> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<UnusedAbstractValue, LiveVarStore> p) {
    RegularTransferResult<UnusedAbstractValue, LiveVarStore> transferResult =
        (RegularTransferResult<UnusedAbstractValue, LiveVarStore>)
            super.visitMethodInvocation(n, p);
    LiveVarStore store = transferResult.getRegularStore();
    for (Node arg : n.getArguments()) {
      store.addUseInExpression(arg);
    }
    return transferResult;
  }

  @Override
  public RegularTransferResult<UnusedAbstractValue, LiveVarStore> visitObjectCreation(
      ObjectCreationNode n, TransferInput<UnusedAbstractValue, LiveVarStore> p) {
    RegularTransferResult<UnusedAbstractValue, LiveVarStore> transferResult =
        (RegularTransferResult<UnusedAbstractValue, LiveVarStore>) super.visitObjectCreation(n, p);
    LiveVarStore store = transferResult.getRegularStore();
    for (Node arg : n.getArguments()) {
      store.addUseInExpression(arg);
    }
    return transferResult;
  }

  @Override
  public RegularTransferResult<UnusedAbstractValue, LiveVarStore> visitReturn(
      ReturnNode n, TransferInput<UnusedAbstractValue, LiveVarStore> p) {
    RegularTransferResult<UnusedAbstractValue, LiveVarStore> transferResult =
        (RegularTransferResult<UnusedAbstractValue, LiveVarStore>) super.visitReturn(n, p);
    Node result = n.getResult();
    if (result != null) {
      LiveVarStore store = transferResult.getRegularStore();
      store.addUseInExpression(result);
    }
    return transferResult;
  }

  /**
   * Update the information of live variables from an assignment statement.
   *
   * @param variable the variable that should be killed
   * @param expression the expression in which the variables should be added
   * @param store the live variable store
   */
  private void processLiveVarInAssignment(Node variable, Node expression, LiveVarStore store) {
    store.killLiveVar(new LiveVarNode(variable));
    store.addUseInExpression(expression);
  }
}
