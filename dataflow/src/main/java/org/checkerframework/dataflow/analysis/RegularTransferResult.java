package org.checkerframework.dataflow.analysis;

import java.util.Map;
import java.util.StringJoiner;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of a {@link TransferResult} with just one non-exceptional store. The result of
 * {@code getThenStore} and {@code getElseStore} is equal to the only underlying store.
 *
 * @param <S> the {@link Store} used to keep track of intermediate results
 */
public class RegularTransferResult<A extends AbstractValue<A>, S extends Store<S>>
        extends TransferResult<A, S> {

    /** The regular result store. */
    protected final S store;

    /** Whether the store changed. */
    private final boolean storeChanged;

    /**
     * *
     *
     * <p><em>Exceptions</em>: If the corresponding {@link
     * org.checkerframework.dataflow.cfg.node.Node} throws an exception, then it is assumed that no
     * special handling is necessary and the store before the corresponding {@link
     * org.checkerframework.dataflow.cfg.node.Node} will be passed along any exceptional edge.
     *
     * <p><em>Aliasing</em>: {@code resultStore} is not allowed to be used anywhere outside of this
     * class (including use through aliases). Complete control over the object is transfered to this
     * class.
     *
     * @see #RegularTransferResult(AbstractValue, Store, Map, boolean)
     */
    public RegularTransferResult(@Nullable A value, S resultStore, boolean storeChanged) {
        this(value, resultStore, null, storeChanged);
    }

    /**
     * See {@link #RegularTransferResult(AbstractValue, Store, Map, boolean)}.
     *
     * @see #RegularTransferResult(AbstractValue, Store, Map, boolean)
     */
    public RegularTransferResult(@Nullable A value, S resultStore) {
        this(value, resultStore, false);
    }

    /**
     * See {@link #RegularTransferResult(AbstractValue, Store, Map, boolean)}.
     *
     * @see #RegularTransferResult(AbstractValue, Store, Map, boolean)
     */
    public RegularTransferResult(
            @Nullable A value, S resultStore, Map<TypeMirror, S> exceptionalStores) {
        this(value, resultStore, exceptionalStores, false);
    }

    /**
     * Create a {@code TransferResult} with {@code resultStore} as the resulting store. If the
     * corresponding {@link org.checkerframework.dataflow.cfg.node.Node} is a boolean node, then
     * {@code resultStore} is used for both the 'then' and 'else' edge.
     *
     * <p>For the meaning of storeChanged, see {@link
     * org.checkerframework.dataflow.analysis.TransferResult#storeChanged}.
     *
     * <p><em>Exceptions</em>: If the corresponding {@link
     * org.checkerframework.dataflow.cfg.node.Node} throws an exception, then the corresponding
     * store in {@code exceptionalStores} is used. If no exception is found in {@code
     * exceptionalStores}, then it is assumed that no special handling is necessary and the store
     * before the corresponding {@link org.checkerframework.dataflow.cfg.node.Node} will be passed
     * along any exceptional edge.
     *
     * <p><em>Aliasing</em>: {@code resultStore} and any store in {@code exceptionalStores} are not
     * allowed to be used anywhere outside of this class (including use through aliases). Complete
     * control over the objects is transfered to this class.
     */
    public RegularTransferResult(
            @Nullable A value,
            S resultStore,
            @Nullable Map<TypeMirror, S> exceptionalStores,
            boolean storeChanged) {
        super(value, exceptionalStores);
        this.store = resultStore;
        this.storeChanged = storeChanged;
    }

    /** The regular result store. */
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
        StringJoiner result = new StringJoiner(System.lineSeparator());
        result.add("RegularTransferResult(");
        result.add("  resultValue = " + resultValue);
        result.add("  store = " + store);
        result.add(")");
        return result.toString();
    }

    /**
     * See {@link org.checkerframework.dataflow.analysis.TransferResult#storeChanged()}.
     *
     * @see org.checkerframework.dataflow.analysis.TransferResult#storeChanged()
     */
    @Override
    public boolean storeChanged() {
        return storeChanged;
    }
}
