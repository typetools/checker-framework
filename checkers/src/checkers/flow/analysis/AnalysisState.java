package checkers.flow.analysis;

import checkers.flow.cfg.node.Node;

/**
 * An interface that allows to query the state of a dataflow analysis.
 * 
 * @author Stefan Heule
 * 
 */
public interface AnalysisState<A extends AbstractValue> {

	/**
	 * @return The abstract value of a node. Returns 'top' of no information is
	 *         available.
	 */
	A getValue(Node n);

	/**
	 * Set the abstract value of a node. If other information is already
	 * present, then this information is overriden.
	 */
	void setValue(Node n, A val);

	/**
	 * Set the abstract value of a node. If other information is already
	 * present, then this new information {@code val} is incorporated by taking
	 * the least upper bound.
	 */
	void addValue(Node n, A val);
}
