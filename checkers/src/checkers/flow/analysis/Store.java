package checkers.flow.analysis;

public interface Store<S> {

	/** Create an exact copy of this store. */
	S copy();

	/**
	 * Compute the least upper bound of two stores.
	 * 
	 * <p>
	 * 
	 * <em>Important</em>: This method must fullfil the following contract:
	 * <ul>
	 * <li>Does not change {@code this}.</li>
	 * <li>Does not change {@code other}.</li>
	 * <li>Returns a fresh object which is not aliased yet.</li>
	 * <li>Returns an object of the same (dynamic) type as {@code this}, even if
	 * the signature is more permissive.</li>
	 * <li>Is commutative.</li>
	 * </ul>
	 */
	S leastUpperBound(S other);
}
