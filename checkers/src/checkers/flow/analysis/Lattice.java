package checkers.flow.analysis;

public interface Lattice<A extends AbstractValue, S extends Store<A>> {

	/**
	 * Compute the least upper bound of two stores.
	 * 
	 * <p>
	 * 
	 * <em>Important</em>: This method is not allowed to change <code>a</code>
	 * or <code>b</code>, and is required to return a fresh store that is not
	 * aliased. Furthermore the method must be commutative with regard to
	 * <code>a</code> and <code>b</code>.
	 */
	S leastUpperBound(S a, S b);
}
