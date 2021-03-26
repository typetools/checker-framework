package org.checkerframework.dataflow.analysis;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;

/**
 * Interface of a forward transfer function for the abstract interpretation used for the forward
 * flow analysis.
 *
 * <p><em>Important</em>: The individual transfer functions ( {@code visit*}) are allowed to use
 * (and modify) the stores contained in the argument passed; the ownership is transferred from the
 * caller to that function.
 *
 * @param <V> the abstract value type to be tracked by the analysis
 * @param <S> the store type used in the analysis
 */
public interface ForwardTransferFunction<V extends AbstractValue<V>, S extends Store<S>>
    extends TransferFunction<V, S> {

  /**
   * Returns the initial store to be used by the org.checkerframework.dataflow analysis. {@code
   * parameters} is non-null if the underlying AST is a method.
   *
   * @param underlyingAST an abstract syntax tree
   * @param parameters a list of local variable nodes
   * @return the initial store
   */
  S initialStore(UnderlyingAST underlyingAST, @Nullable List<LocalVariableNode> parameters);
}
