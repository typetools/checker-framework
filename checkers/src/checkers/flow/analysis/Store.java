package checkers.flow.analysis;

public interface Store<A extends AbstractValue> {
	
	/** Create an exact copy of this store. */
	Store<A> copy();
	
	/**
	 * Compute the least upper bound of two stores.
	 * 
	 * <p>
	 * 
	 * <em>Important</em>: This method is not allowed to change <code>this</code>
	 * or <code>other</code>, and is required to return a fresh store that is not
	 * aliased. Furthermore the method must be commutative.
	 */
	Store<A> leastUpperBound(Store<A> other);
}
