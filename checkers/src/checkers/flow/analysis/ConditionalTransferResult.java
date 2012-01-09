package checkers.flow.analysis;

import java.util.Map;

/**
 * {@code ConditionalTransferResult} is used as the result type of the
 * individual transfer functions of a {@link TransferFunction}. Unlike
 * {@link TransferResult}, two different stores can potentially be returned for
 * the 'then' and 'else' edge. The corresponding {@link Node} must be of boolean
 * type and part of a conditional basic block.
 * 
 * @author Stefan Heule
 * 
 * @param <S>
 *            The {@link Store} used to keep track of intermediate results.
 */
public class ConditionalTransferResult<S extends Store<S>> extends
		TransferResult<S> {

	/** The 'then' result store. */
	protected S thenStore;

	/** The 'else' result store. */
	protected S elseStore;

	/**
	 * Create a {@code ConditionalTransferResult} with {@code thenStore} as the
	 * resulting store if the corresponding {@link Node} evaluates to
	 * {@code true} and {@code elseStore} otherwise.
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
	 * <em>Aliasing</em>: {@code thenStore} and {@code elseStore} are not
	 * allowed to be used anywhere outside of this class (including use through
	 * aliases). Complete control over the objects is transfered to this class.
	 */
	public ConditionalTransferResult(S thenStore, S elseStore) {
		super(null);
		this.thenStore = thenStore;
		this.elseStore = elseStore;
	}

	/**
	 * Create a {@code ConditionalTransferResult} with {@code thenStore} as the
	 * resulting store if the corresponding {@link Node} evaluates to
	 * {@code true} and {@code elseStore} otherwise.
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
	 * <em>Aliasing</em>: {@code thenStore}, {@code elseStore}, and any store in
	 * {@code exceptionalStores} are not allowed to be used anywhere outside of
	 * this class (including use through aliases). Complete control over the
	 * objects is transfered to this class.
	 */
	public ConditionalTransferResult(S thenStore, S elseStore,
			Map<Class<? extends Throwable>, S> exceptionalStores) {
		super(null, exceptionalStores);
		this.thenStore = thenStore;
		this.elseStore = elseStore;
	}

	@Override
	public S getRegularStore() {
		return thenStore.leastUpperBound(elseStore);
	}

	@Override
	public S getThenStore() {
		return thenStore;
	}

	@Override
	public S getElseStore() {
		return elseStore;
	}

}
