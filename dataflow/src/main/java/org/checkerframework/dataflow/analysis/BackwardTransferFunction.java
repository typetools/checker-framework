package org.checkerframework.dataflow.analysis;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.ReturnNode;

import java.util.List;

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
     * @param returnNodes the return nodes of the given control flow graph if the underlying AST of
     *     this graph is a method. Otherwise will be set to {@code null}
     * @return the initial store that should be used at the normal exit block
     */
    S initialNormalExitStore(UnderlyingAST underlyingAST, @Nullable List<ReturnNode> returnNodes);

    /**
     * Returns the initial store that should be used at the exceptional exit block or given the
     * underlying AST of a control flow graph.
     *
     * @param underlyingAST the underlying AST of the given control flow graph
     * @return the initial store that should be used at the exceptional exit block
     */
    S initialExceptionalExitStore(UnderlyingAST underlyingAST);
}
