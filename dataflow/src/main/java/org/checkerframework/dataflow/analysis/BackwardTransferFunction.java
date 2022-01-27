package org.checkerframework.dataflow.analysis;

import java.util.List;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.ReturnNode;

/**
 * Interface of a backward transfer function for the abstract interpretation used for the backward
 * flow analysis.
 *
 * <p><em>Important</em>: The individual transfer functions ( {@code visit*}) are allowed to use
 * (and modify) the stores contained in the argument passed; the ownership is transferred from the
 * caller to that function.
 *
 * @param <V> the abstract value type to be tracked by the analysis
 * @param <S> the store type used in the analysis
 */
public interface BackwardTransferFunction<V extends AbstractValue<V>, S extends Store<S>>
    extends TransferFunction<V, S> {

  /**
   * Returns the initial store that should be used at the normal exit block.
   *
   * @param underlyingAST the underlying AST of the given control flow graph
   * @param returnNodes the return nodes of the given control flow graph (an empty list if the
   *     underlying AST is not a method)
   * @return the initial store that should be used at the normal exit block
   */
  S initialNormalExitStore(UnderlyingAST underlyingAST, List<ReturnNode> returnNodes);

  /**
   * Returns the initial store that should be used at the exceptional exit block or given the
   * underlying AST of a control flow graph.
   *
   * @param underlyingAST the underlying AST of the given control flow graph
   * @return the initial store that should be used at the exceptional exit block
   */
  S initialExceptionalExitStore(UnderlyingAST underlyingAST);
}
