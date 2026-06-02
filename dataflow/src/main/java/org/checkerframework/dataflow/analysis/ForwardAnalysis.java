package org.checkerframework.dataflow.analysis;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.plumelib.util.IPair;

/**
 * This interface defines a forward analysis, given a control flow graph and a forward transfer
 * function.
 *
 * @param <V> the abstract value type to be tracked by the analysis
 * @param <S> the store type used in the analysis
 * @param <T> the forward transfer function type that is used to approximate run-time behavior
 */
public interface ForwardAnalysis<
        V extends AbstractValue<V>, S extends Store<S>, T extends ForwardTransferFunction<V, S>>
    extends Analysis<V, S, T> {

  /**
   * Returns stores at return statements. These stores are transfer results at return node. Thus for
   * a forward analysis, these stores contain the analyzed flow information from entry nodes to
   * return nodes.
   *
   * @return the transfer results for each return node in the CFG
   */
  List<IPair<ReturnNode, @Nullable TransferResult<V, S>>> getReturnStatementStores();
}
