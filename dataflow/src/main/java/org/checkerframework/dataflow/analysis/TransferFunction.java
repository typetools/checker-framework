package org.checkerframework.dataflow.analysis;

import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NodeVisitor;

/**
 * Interface of a transfer function for the abstract interpretation used for the flow analysis.
 *
 * <p>A transfer function consists of the following components:
 *
 * <ul>
 *   <li>Initial store method(s) that determines which initial store should be used in the
 *       org.checkerframework.dataflow analysis.
 *   <li>A function for every {@link Node} type that determines the behavior of the
 *       org.checkerframework.dataflow analysis in that case. This method takes a {@link Node} and
 *       an incoming store, and produces a {@link RegularTransferResult}.
 * </ul>
 *
 * <p><em>Note</em>: Initial store method(s) are different between forward and backward transfer
 * functions. Thus, this interface doesn't define any initial store method(s). {@link
 * ForwardTransferFunction} and {@link BackwardTransferFunction} will create their own initial store
 * method(s).
 *
 * <p><em>Important</em>: The individual transfer functions ( {@code visit*}) are allowed to use
 * (and modify) the stores contained in the argument passed; the ownership is transferred from the
 * caller to that function.
 *
 * @param <V> type of the abstract value that is tracked
 * @param <S> the store type used in the analysis
 */
public interface TransferFunction<V extends AbstractValue<V>, S extends Store<S>>
        extends NodeVisitor<TransferResult<V, S>, TransferInput<V, S>> {}
