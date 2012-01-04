package checkers.flow.analysis;

/**
 * An abstract value used in the dataflow analysis.
 * 
 * @author Stefan Heule
 * 
 */
public interface AbstractValue {

	/**
	 * Compute the least upper bound of two stores.
	 * 
	 * <p>
	 * 
	 * <em>Important</em>: This method is not allowed to change <code>this</code>
	 * or <code>other</code>, and is required to return a fresh store that is not
	 * aliased. Furthermore the method must be commutative.
	 */
	AbstractValue leastUpperBound(AbstractValue other);
}
