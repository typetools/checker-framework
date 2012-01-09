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
	 * The store used for exceptions, unless a more specific store is available
	 * in {@code exceptionalStores} (or {@code null} if the corresponding
	 * {@link Node} does not throw any exceptions).
	 */
	protected/* @Nullable */S exceptionalStore;

	/** Has {@code exceptionalStore} already been passed to a client? */
	protected boolean exceptionalStoreUsageCount = false;

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
	 * <em>Exceptions</em>: The corresponding {@link Node} is not allowed to
	 * throw any exceptions.
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
	 * <em>Exceptions</em>: If the corresponding {@link Node} throws any
	 * exceptions, then {@code exceptionalStore} is used as resulting store.
	 * 
	 * <p>
	 * 
	 * <em>Aliasing</em>: {@code resultStore} and {@code exceptionalStore} are
	 * not allowed to be used anywhere outside of this class (including use
	 * through aliases). Complete control over the objects is transfered to this
	 * class.
	 */
	public TransferResult(S resultStore, S exceptionalStore) {
		store = resultStore;
		this.exceptionalStore = exceptionalStore;
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
	 * used. If no such store is present in the map, the default exceptional
	 * store {@code defaultExceptionalStore} will be used.
	 * 
	 * <p>
	 * 
	 * <em>Aliasing</em>: {@code resultStore}, {@code defaultExceptionalStore}
	 * and any store in {@code exceptionalStores} are not allowed to be used
	 * anywhere outside of this class (including use through aliases). Complete
	 * control over the objects is transfered to this class.
	 */
	public TransferResult(S resultStore, S defaultExceptionalStore,
			Map<Class<? extends Throwable>, S> exceptionalStores) {
		store = resultStore;
		exceptionalStore = defaultExceptionalStore;
		this.exceptionalStores = exceptionalStores;
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
	 * used. For any possible exception, a store must be present in the map.
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
	 *         with {@code exception}.
	 */
	public S getExceptionalStore(Class<? extends Throwable> exception) {
		assert exceptionalStores != null;
		if (exceptionalStores.containsKey(exception)) {
			return exceptionalStores.get(exception);
		} else {
			assert exceptionalStore != null : "This TansferResult is supposed to belong to a basic block that does not throw exceptions of type "
					+ exception;
			// only copy exceptionalStore if necessary
			S r = exceptionalStoreUsageCount ? exceptionalStore.copy()
					: exceptionalStore;
			exceptionalStoreUsageCount = true;
			return r;
		}
	}
}
