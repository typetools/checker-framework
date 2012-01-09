package checkers.flow.analysis;

import java.util.List;

import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.NodeVisitor;

import com.sun.source.tree.MethodTree;

/**
 * Interface of a transfer function for the abstract interpretation used for the
 * flow analysis.
 * 
 * <p>
 * 
 * A transfer function consists of the following components:
 * <ul>
 * <li>A method {@code initialStore} that determines which initial store should
 * be used in the dataflow analysis.</li>
 * <li>A function for every {@link Node} type that determines the behavior of
 * the dataflow analysis in that case. This method takes a {@link Node} and an
 * incoming store, and produces an output store.</li>
 * </ul>
 * 
 * <p>
 * 
 * <em>Important</em>: The individual transfer functions ( {@code visit*}) are
 * allowed to use (and modify) the store passed as argument in any way; the
 * ownership is transfered from the caller to that function. The return value
 * (also a store) is required to not be aliased and ownership is thus transfered
 * to the caller. In particular, this allows these method to return (a possibly
 * modified version) of its argument.
 * 
 * @author Stefan Heule
 * 
 * @param <A>
 *            The {@link Store} used to keep track of intermediate results.
 */
public interface TransferFunction<S extends Store<S>> extends NodeVisitor<S, S> {

	/** @return The initial store to be used by the dataflow analysis. */
	S initialStore(MethodTree tree, List<LocalVariableNode> parameters);
}
