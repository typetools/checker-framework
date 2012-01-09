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
	 * <em>Exceptions</em>: The corresponding {@link Node} is not allowed to
	 * throw any exceptions.
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
	 * <em>Exceptions</em>: The corresponding {@link Node} is not allowed to
	 * throw any exceptions.
	 * 
	 * <p>
	 * 
	 * <em>Aliasing</em>: {@code thenStore}, {@code elseStore}, and
	 * {@code exceptionalStore} are not allowed to be used anywhere outside of
	 * this class (including use through aliases). Complete control over the
	 * objects is transfered to this class.
	 */
	public ConditionalTransferResult(S thenStore, S elseStore,
			S exceptionalStore) {
		super(null, exceptionalStore);
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
	 * <em>Exceptions</em>: The corresponding {@link Node} is not allowed to
	 * throw any exceptions.
	 * 
	 * <p>
	 * 
	 * <em>Aliasing</em>: {@code thenStore}, {@code elseStore},
	 * {@code defaultExceptionalStore}, and any store in
	 * {@code exceptionalStores} are not allowed to be used anywhere outside of
	 * this class (including use through aliases). Complete control over the
	 * objects is transfered to this class.
	 */
	public ConditionalTransferResult(S thenStore, S elseStore,
			S defaultExceptionalStore,
			Map<Class<? extends Throwable>, S> exceptionalStores) {
		super(null, defaultExceptionalStore, exceptionalStores);
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
	 * <em>Exceptions</em>: The corresponding {@link Node} is not allowed to
	 * throw any exceptions.
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
