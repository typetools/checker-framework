package org.checkerframework.dataflow.analysis;

import java.util.Map;
import java.util.StringJoiner;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of a {@link TransferResult} with just one non-exceptional store. The result of
 * {@code getThenStore} and {@code getElseStore} is equal to the only underlying store.
 *
 * @param <V> type of the abstract value that is tracked
 * @param <S> the store type used in the analysis
 */
public class RegularTransferResult<V extends AbstractValue<V>, S extends Store<S>>
        extends TransferResult<V, S> {

    /** The regular result store. */
    protected final S store;

    /**
     * Whether the store changed; see {@link
     * org.checkerframework.dataflow.analysis.TransferResult#storeChanged}.
     */
    private final boolean storeChanged;

    /**
     * Create a new {@link #RegularTransferResult(AbstractValue, Store, Map, boolean)}, using {@code
     * null} for {@link org.checkerframework.dataflow.analysis.TransferResult#exceptionalStores}.
     *
     * <p><em>Exceptions</em>: If the corresponding {@link
     * org.checkerframework.dataflow.cfg.node.Node} throws an exception, then it is assumed that no
     * special handling is necessary and the store before the corresponding {@link
     * org.checkerframework.dataflow.cfg.node.Node} will be passed along any exceptional edge.
     *
     * <p><em>Aliasing</em>: {@code resultStore} is not allowed to be used anywhere outside of this
     * class (including use through aliases). Complete control over the object is transferred to
     * this class.
     *
     * @param value the abstract value produced by the transfer function
     * @param resultStore regular result store
     * @param storeChanged whether the store changed; see {@link
     *     org.checkerframework.dataflow.analysis.TransferResult#storeChanged}
     * @see #RegularTransferResult(AbstractValue, Store, Map, boolean)
     */
    public RegularTransferResult(@Nullable V value, S resultStore, boolean storeChanged) {
        this(value, resultStore, null, storeChanged);
    }

    /**
     * Create a new {@link #RegularTransferResult(AbstractValue, Store, Map, boolean)}, using {@code
     * null} for {@link org.checkerframework.dataflow.analysis.TransferResult#exceptionalStores} and
     * {@code false} for {@link org.checkerframework.dataflow.analysis.TransferResult#storeChanged}.
     *
     * @param value the abstract value produced by the transfer function
     * @param resultStore regular result store
     * @see #RegularTransferResult(AbstractValue, Store, Map, boolean)
     */
    public RegularTransferResult(@Nullable V value, S resultStore) {
        this(value, resultStore, false);
    }

    /**
     * Create a new {@link #RegularTransferResult(AbstractValue, Store, Map, boolean)}, using {@code
     * false} for {@link org.checkerframework.dataflow.analysis.TransferResult#storeChanged}.
     *
     * @param value the abstract value produced by the transfer function
     * @param resultStore the regular result store
     * @param exceptionalStores the stores in case the basic block throws an exception, or null if
     *     the basic block does not throw any exceptions
     * @see #RegularTransferResult(AbstractValue, Store, Map, boolean)
     */
    public RegularTransferResult(
            @Nullable V value, S resultStore, Map<TypeMirror, S> exceptionalStores) {
        this(value, resultStore, exceptionalStores, false);
    }

    /**
     * Create a {@code TransferResult} with {@code resultStore} as the resulting store. If the
     * corresponding {@link org.checkerframework.dataflow.cfg.node.Node} is a boolean node, then
     * {@code resultStore} is used for both the 'then' and 'else' edge.
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
     * control over the objects is transferred to this class.
     *
     * @param value the abstract value produced by the transfer function
     * @param resultStore the regular result store
     * @param exceptionalStores the stores in case the basic block throws an exception, or null if
     *     the basic block does not throw any exceptions
     * @param storeChanged see {@link
     *     org.checkerframework.dataflow.analysis.TransferResult#storeChanged}
     */
    public RegularTransferResult(
            @Nullable V value,
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
        // "toString().trim()" works around bug where toString ends with a newline.
        result.add("  store = " + store.toString().trim());
        result.add("  )");
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
