package org.checkerframework.dataflow.reachingdef;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.ForwardTransferFunction;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.AbstractNodeVisitor;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;

/**
 * The reaching definition transfer function. The transfer function processes the
 * ReachingDefinitionNode in ReachingDefinitionStore, killing the node with the same LHS and putting
 * the newly generated node into the store. See the dataflow manual for more details.
 */
public class ReachingDefinitionTransfer
    extends AbstractNodeVisitor<
        TransferResult<UnusedAbstractValue, ReachingDefinitionStore>,
        TransferInput<UnusedAbstractValue, ReachingDefinitionStore>>
    implements ForwardTransferFunction<UnusedAbstractValue, ReachingDefinitionStore> {

  /** Creates a new ReachingDefinitionTransfer. */
  public ReachingDefinitionTransfer() {}

  @Override
  public ReachingDefinitionStore initialStore(
      UnderlyingAST underlyingAST, @Nullable List<LocalVariableNode> parameters) {
    return new ReachingDefinitionStore();
  }

  @Override
  public RegularTransferResult<UnusedAbstractValue, ReachingDefinitionStore> visitNode(
      Node n, TransferInput<UnusedAbstractValue, ReachingDefinitionStore> p) {
    return new RegularTransferResult<>(null, p.getRegularStore());
  }

  @Override
  public RegularTransferResult<UnusedAbstractValue, ReachingDefinitionStore> visitAssignment(
      AssignmentNode n, TransferInput<UnusedAbstractValue, ReachingDefinitionStore> p) {
    RegularTransferResult<UnusedAbstractValue, ReachingDefinitionStore> transferResult =
        (RegularTransferResult<UnusedAbstractValue, ReachingDefinitionStore>)
            super.visitAssignment(n, p);
    processDefinition(n, transferResult.getRegularStore());
    return transferResult;
  }

  /**
   * Updates a reaching definition node in the store from an assignment statement.
   *
   * @param def the definition that should be put into the store
   * @param store the reaching definition store
   */
  private void processDefinition(AssignmentNode def, ReachingDefinitionStore store) {
    store.killDef(def.getTarget());
    store.putDef(new ReachingDefinitionNode(def));
  }
}
