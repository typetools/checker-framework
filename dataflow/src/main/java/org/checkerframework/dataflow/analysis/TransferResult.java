package org.checkerframework.dataflow.analysis;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

import javax.lang.model.type.TypeMirror;

/**
 * {@code TransferResult} is used as the result type of the individual transfer functions of a
 * {@link TransferFunction}. It always belongs to the result of the individual transfer function for
 * a particular {@link org.checkerframework.dataflow.cfg.node.Node}, even though that {@code
 * org.checkerframework.dataflow.cfg.node.Node} is not explicitly stored in {@code TransferResult}.
 *
 * <p>A {@code TransferResult} consists of a result value, plus one or more stores. It contains one
 * or two stores (for 'then' and 'else'), and zero or more stores with a cause ({@link TypeMirror}).
 *
 * @param <V> type of the abstract value that is tracked
 * @param <S> the store type used in the analysis
 */
public abstract class TransferResult<V extends AbstractValue<V>, S extends Store<S>> {

    /**
     * The abstract value of the {@link org.checkerframework.dataflow.cfg.node.Node} associated with
     * this {@link TransferResult}, or {@code null} if no value has been produced.
     */
    protected @Nullable V resultValue;

    /**
     * The stores in case the basic block throws an exception (or {@code null} if the corresponding
     * {@link org.checkerframework.dataflow.cfg.node.Node} does not throw any exceptions). Does not
     * necessarily contain a store for every exception, in which case the in-store will be used.
     */
    protected final @Nullable Map<TypeMirror, S> exceptionalStores;

    /**
     * Create a new TransferResult, given {@link #resultValue} and {@link #exceptionalStores}.
     *
     * @param resultValue the abstract value of the {@link
     *     org.checkerframework.dataflow.cfg.node.Node} associated with this {@link TransferResult}
     * @param exceptionalStores the stores in case the basic block throws an exception (or {@code
     *     null} if the corresponding {@link org.checkerframework.dataflow.cfg.node.Node} does not
     *     throw any exceptions)
     */
    protected TransferResult(
            @Nullable V resultValue, @Nullable Map<TypeMirror, S> exceptionalStores) {
        this.resultValue = resultValue;
        this.exceptionalStores = exceptionalStores;
    }

    /**
     * Returns the abstract value produced by the transfer function, {@code null} otherwise.
     *
     * @return the abstract value produced by the transfer function, {@code null} otherwise
     */
    public @Nullable V getResultValue() {
        return resultValue;
    }

    /**
     * Set the value of {@link #resultValue}.
     *
     * @param resultValue the abstract value of the {@link
     *     org.checkerframework.dataflow.cfg.node.Node} associated with this {@link TransferResult}
     */
    public void setResultValue(V resultValue) {
        this.resultValue = resultValue;
    }

    /**
     * Returns the regular result store produced if no exception is thrown by the {@link
     * org.checkerframework.dataflow.cfg.node.Node} corresponding to this transfer function result.
     *
     * @return the regular result store produced if no exception is thrown by the {@link
     *     org.checkerframework.dataflow.cfg.node.Node} corresponding to this transfer function
     *     result
     */
    public abstract S getRegularStore();

    /**
     * Returns the result store produced if the {@link org.checkerframework.dataflow.cfg.node.Node}
     * this result belongs to evaluates to {@code true}.
     *
     * @return the result store produced if the {@link org.checkerframework.dataflow.cfg.node.Node}
     *     this result belongs to evaluates to {@code true}
     */
    public abstract S getThenStore();

    /**
     * Returns the result store produced if the {@link org.checkerframework.dataflow.cfg.node.Node}
     * this result belongs to evaluates to {@code false}.
     *
     * @return the result store produced if the {@link org.checkerframework.dataflow.cfg.node.Node}
     *     this result belongs to evaluates to {@code false}
     */
    public abstract S getElseStore();

    /**
     * Returns the store that flows along the outgoing exceptional edge labeled with {@code
     * exception} (or {@code null} if no special handling is required for exceptional edges).
     *
     * @param exception an exception type
     * @return the store that flows along the outgoing exceptional edge labeled with {@code
     *     exception} (or {@code null} if no special handling is required for exceptional edges)
     */
    public @Nullable S getExceptionalStore(TypeMirror exception) {
        if (exceptionalStores == null) {
            return null;
        }
        return exceptionalStores.get(exception);
    }

    /**
     * Returns a Map of {@link TypeMirror} to {@link Store}, {@code null} otherwise.
     *
     * @return a Map of {@link TypeMirror} to {@link Store}, {@code null} otherwise
     * @see TransferResult#getExceptionalStore(TypeMirror)
     */
    public @Nullable Map<TypeMirror, S> getExceptionalStores() {
        return exceptionalStores;
    }

    /**
     * Returns {@code true} if and only if this transfer result contains two stores that are
     * potentially not equal. Note that the result {@code true} does not imply that {@code
     * getRegularStore} cannot be called (or vice versa for {@code false}). Rather, it indicates
     * that {@code getThenStore} or {@code getElseStore} can be used to give more precise results.
     * Otherwise, if the result is {@code false}, then all three methods {@code getRegularStore},
     * {@code getThenStore}, and {@code getElseStore} return equivalent stores.
     *
     * @return {@code true} if and only if this transfer result contains two stores that are
     *     potentially not equal
     */
    public abstract boolean containsTwoStores();

    /**
     * Returns {@code true} if and only if the transfer function returning this transfer result
     * changed the regularStore, elseStore, or thenStore.
     *
     * @return {@code true} if and only if the transfer function returning this transfer result
     *     changed the regularStore, elseStore, or thenStore
     */
    public abstract boolean storeChanged();
}
