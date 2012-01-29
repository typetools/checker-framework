package checkers.flow.analysis;

import checkers.flow.cfg.node.Node;

/**
 * {@code TransferInput} is used as the input type of the individual transfer
 * functions of a {@link TransferFunction}.
 * 
 * <p>
 * 
 * A {@code TransferInput} contains one or two stores. If two stores are
 * present, one belongs to 'then', and the other to 'else'.
 * 
 * @author Stefan Heule
 * 
 * @param <S>
 *            The {@link Store} used to keep track of intermediate results.
 */
public class TransferInput<S extends Store<S>> {

	/**
	 * The regular result store (or {@code null} if none is present). The
	 * following invariant is maintained:
	 * 
	 * <pre>
	 * store == null <==> thenStore != null && elseStore != null
	 * </pre>
	 */
	protected/* @Nullable */S store;

	/**
	 * The 'then' result store (or {@code null} if none is present). The
	 * following invariant is maintained:
	 * 
	 * <pre>
	 * store == null <==> thenStore != null && elseStore != null
	 * </pre>
	 */
	protected/* @Nullable */S thenStore;

	/**
	 * The 'else' result store (or {@code null} if none is present). The
	 * following invariant is maintained:
	 * 
	 * <pre>
	 * store == null <==> thenStore != null && elseStore != null
	 * </pre>
	 */
	protected/* @Nullable */S elseStore;

	/**
	 * Create a {@link TransferInput}, given a {@link TransferResult}.
	 * 
	 * <p>
	 * 
	 * <em>Aliasing</em>: The stores returned by any methods of {@code to} will
	 * be stored internally and are not allowed to be used elsewhere. Full
	 * control of them is transfered to this object.
	 */
	public TransferInput(TransferResult<S> to) {
		if (to.containsTwoStores()) {
			thenStore = to.getThenStore();
			elseStore = to.getElseStore();
		} else {
			store = to.getRegularStore();
		}
	}

	/**
	 * Create a {@link TransferInput}, given a store.
	 * 
	 * <p>
	 * 
	 * <em>Aliasing</em>: The store {@code s} will be stored internally and is
	 * not allowed to be used elsewhere. Full control of them is transfered to
	 * this object.
	 */
	public TransferInput(S s) {
		store = s;
	}

	/**
	 * Create a {@link TransferInput}, given two stores.
	 * 
	 * <p>
	 * 
	 * <em>Aliasing</em>: The two stores {@code s1} and {@code s2} will be
	 * stored internally and are not allowed to be used elsewhere. Full control
	 * of them is transfered to this object.
	 */
	public TransferInput(S s1, S s2) {
		thenStore = s1;
		elseStore = s2;
	}

	/**
	 * Copy constructor.
	 */
	protected TransferInput(TransferInput<S> from) {
		if (from.store == null) {
			thenStore = from.thenStore.copy();
			elseStore = from.elseStore.copy();
		} else {
			store = from.store.copy();
		}
	}

	/**
	 * @return The regular result store produced if no exception is thrown by
	 *         the {@link Node} corresponding to this transfer function result.
	 */
	public S getRegularStore() {
		if (store == null) {
			return thenStore.leastUpperBound(elseStore);
		} else {
			return store;
		}
	}

	/**
	 * @return The result store produced if the {@link Node} this result belongs
	 *         to evaluates to {@code true}.
	 */
	public S getThenStore() {
		if (store == null) {
			return thenStore;
		}
		return store;
	}

	/**
	 * @return The result store produced if the {@link Node} this result belongs
	 *         to evaluates to {@code false}.
	 */
	public S getElseStore() {
		if (store == null) {
			return elseStore;
		}
		// copy the store such that it is the same as the result of getThenStore
		// (that is, identical according to equals), but two different objects.
		return store.copy();
	}

	/**
	 * @return {@code true} if and only if this transfer input contains two
	 *         stores that are potentially not equal. Note that the result
	 *         {@code true} does not imply that {@code getRegularStore} cannot
	 *         be called (or vice versa for {@code false}). Rather, it indicates
	 *         that {@code getThenStore} or {@code getElseStore} can be used to
	 *         give more precise results. Otherwise, if the result is
	 *         {@code false}, then all three methods {@code getRegularStore},
	 *         {@code getThenStore}, and {@code getElseStore} return equivalent
	 *         stores.
	 */
	public boolean containsTwoStores() {
            return (thenStore != null && elseStore != null);
        }

	/** @return An exact copy of this store. */
	public TransferInput<S> copy() {
		return new TransferInput<S>(this);
	}

	/**
	 * Compute the least upper bound of two stores.
	 * 
	 * <p>
	 * 
	 * <em>Important</em>: This method must fulfill the same contract as
	 * {@code leastUpperBound} of {@link Store}.
	 */
	public TransferInput<S> leastUpperBound(TransferInput<S> other) {
		if (store == null) {
			S newThenStore = thenStore.leastUpperBound(other.getThenStore());
			S newElseStore = elseStore.leastUpperBound(other.getElseStore());
			return new TransferInput<>(newThenStore, newElseStore);
		} else {
			if (other.store == null) {
				// make sure we do not lose precision and keep two stores if at
				// least one of the two TransferInput's has two stores.
				return other.leastUpperBound(this);
			}
			return new TransferInput<>(store.leastUpperBound(other
					.getRegularStore()));
		}
	}
	
	@Override
	public String toString() {
		if (store == null) {
			return "[then="+thenStore+", else="+elseStore+"]";
		} else {
			return "["+store+"]";
		}
	}

}
