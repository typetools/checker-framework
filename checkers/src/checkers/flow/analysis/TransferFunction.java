package checkers.flow.analysis;

import checkers.flow.cfg.node.NodeVisitor;

/**
 * Interface of a transfer function for the abstract interpretation used for the
 * flow analysis.
 * 
 * @author Stefan Heule
 * 
 * @param <A>
 *            The {@link Store} used to keep track of intermediate results.
 */
public interface TransferFunction<A extends AbstractValue, S extends Store<A>> extends NodeVisitor<S, S> {

}
