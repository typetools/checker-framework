package checkers.flow.analysis;

import java.util.Map;

/**
 * {@code TransferResult} is used as the result type of the individual transfer
 * functions of a {@link TransferFunction}. It always belongs to the result of
 * the individual transfer function for a particular {@link Node}, even though
 * that {@code Node} is not explicitly store in {@code TransferResult}.
 * 
 * @author Stefan Heule
 * 
 * @param <S>
 *            The {@link Store} used to keep track of intermediate results.
 */
public class TransferResult<S extends Store<S>> {

	/** The regular result store. */
	protected S store;

	/**
	 * The stores in case the basic block throws an exception (or {@code null}
	 * if the corresponding {@link Node} does not throw any exceptions). Does
	 * not necessarily contain a store for every exception, in which case the
	 * in-store will be used.
	 */
	protected/* @Nullable */Map<Class<? extends Throwable>, S> exceptionalStores;

	/**
	 * Create a {@code TransferResult} with {@code resultStore} as the resulting
	 * store. If the corresponding {@link Node} is a boolean node, then
	 * {@code resultStore} is used for both the 'then' and 'else' edge.
	 * 
	 * <p>
	 * 
	 * <em>Exceptions</em>: If the corresponding {@link Node} throws an
	 * exception, then it is assumed that no special handling is necessary and
	 * the store before the corresponding {@link Node} will be passed along any
	 * exceptional edge.
	 * 
	 * <p>
	 * 
	 * <em>Aliasing</em>: {@code resultStore} is not allowed to be used anywhere
	 * outside of this class (including use through aliases). Complete control
	 * over the object is transfered to this class.
	 */
	public TransferResult(S resultStore) {
		store = resultStore;
	}

	/**
	 * Create a {@code TransferResult} with {@code resultStore} as the resulting
	 * store. If the corresponding {@link Node} is a boolean node, then
	 * {@code resultStore} is used for both the 'then' and 'else' edge.
	 * 
	 * <p>
	 * 
	 * <em>Exceptions</em>: If the corresponding {@link Node} throws an
	 * exception, then the corresponding store in {@code exceptionalStores} is
	 * used. If no exception is found in {@code exceptionalStores}, then it is
	 * assumed that no special handling is necessary and the store before the
	 * corresponding {@link Node} will be passed along any exceptional edge.
	 * 
	 * <p>
	 * 
	 * <em>Aliasing</em>: {@code resultStore} and any store in
	 * {@code exceptionalStores} are not allowed to be used anywhere outside of
	 * this class (including use through aliases). Complete control over the
	 * objects is transfered to this class.
	 */
	public TransferResult(S resultStore,
			Map<Class<? extends Throwable>, S> exceptionalStores) {
		store = resultStore;
		this.exceptionalStores = exceptionalStores;
	}

	/**
	 * @return The regular result store produced if no exception is thrown by
	 *         the {@link Node} corresponding to this transfer function result.
	 */
	public S getRegularStore() {
		return store;
	}

	/**
	 * @return The result store produced if the {@link Node} this result belongs
	 *         to evaluates to {@code true}.
	 */
	public S getThenStore() {
		return store;
	}

	/**
	 * @return The result store produced if the {@link Node} this result belongs
	 *         to evaluates to {@code false}.
	 */
	public S getElseStore() {
		// copy the store such that it is the same as the result of getThenStore
		// (that is, identical according to equals), but two different objects.
		return store.copy();
	}

	/**
	 * @return The store that flows along the outgoing exceptional edge labeled
	 *         with {@code exception} (or {@code null} if no special handling is
	 *         required for exceptional edges (cf. constructor)).
	 */
	public/* @Nullable */S getExceptionalStore(
			Class<? extends Throwable> exception) {
		if (exceptionalStores == null) {
			return null;
		}
		return exceptionalStores.get(exception);
	}
}
