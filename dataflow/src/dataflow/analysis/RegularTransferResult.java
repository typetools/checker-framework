package dataflow.analysis;

import java.util.Map;

import javax.lang.model.type.TypeMirror;

/**
 * Implementation of a {@link TransferResult} with just one non-exceptional
 * store. The result of {@code getThenStore} and {@code getElseStore} is equal
 * to the only underlying store.
 * 
 * @author Stefan Heule
 * 
 * @param <S>
 *            The {@link Store} used to keep track of intermediate results.
 */
public class RegularTransferResult<A extends AbstractValue<A>, S extends Store<S>>
        extends TransferResult<A, S> {

    /** The regular result store. */
    protected S store;

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
    public RegularTransferResult(A value, S resultStore) {
        super(value);
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
    public RegularTransferResult(A value, S resultStore,
            Map<TypeMirror, S> exceptionalStores) {
        super(value);
        this.store = resultStore;
        this.exceptionalStores = exceptionalStores;
    }

    @Override
    public S getRegularStore() {
        return store;
    }

    @Override
    public S getThenStore() {
        return store;
    }

    @Override
    public S getElseStore() {
        // copy the store such that it is the same as the result of getThenStore
        // (that is, identical according to equals), but two different objects.
        return store.copy();
    }

    @Override
    public boolean containsTwoStores() {
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("RegularTransferResult(");
        result.append(System.getProperty("line.separator"));
        result.append("resultValue = " + resultValue);
        result.append(System.getProperty("line.separator"));
        result.append("store = " + store);
        result.append(System.getProperty("line.separator"));
        result.append(")");
        return result.toString();
    }
}
