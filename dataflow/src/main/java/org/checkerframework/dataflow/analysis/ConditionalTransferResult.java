package org.checkerframework.dataflow.analysis;

import java.util.Map;
import java.util.StringJoiner;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plumelib.util.StringsPlume;

/**
 * Implementation of a {@link TransferResult} with two non-exceptional stores. The 'then' store
 * contains information valid when the previous boolean-valued expression was true, and the 'else'
 * store contains information valid when the expression was false.
 *
 * <p>{@link getRegularStore} returns the least upper bound of the two underlying stores.
 *
 * @param <V> type of the abstract value that is tracked
 * @param <S> the store type used in the analysis
 */
public class ConditionalTransferResult<V extends AbstractValue<V>, S extends Store<S>>
    extends TransferResult<V, S> {

  /** Whether the store changed. */
  private final boolean storeChanged;

  /** The 'then' result store. */
  protected final S thenStore;

  /** The 'else' result store. */
  protected final S elseStore;

  /**
   * Create a new {@link #ConditionalTransferResult(AbstractValue, Store, Store, Map, boolean)},
   * using {@code null} for {@link #exceptionalStores}.
   *
   * <p><em>Exceptions</em>: If the corresponding {@link
   * org.checkerframework.dataflow.cfg.node.Node} throws an exception, then it is assumed that no
   * special handling is necessary and the store before the corresponding {@link
   * org.checkerframework.dataflow.cfg.node.Node} will be passed along any exceptional edge.
   *
   * <p><em>Aliasing</em>: {@code thenStore} and {@code elseStore} are not allowed to be used
   * anywhere outside of this class (including use through aliases). Complete control over the
   * objects is transferred to this class.
   *
   * @param value the abstract value produced by the transfer function
   * @param thenStore 'then' result store
   * @param elseStore 'else' result store
   * @param storeChanged whether the store changed
   * @see #ConditionalTransferResult(AbstractValue, Store, Store, Map, boolean)
   */
  public ConditionalTransferResult(
      @Nullable V value, S thenStore, S elseStore, boolean storeChanged) {
    this(value, thenStore, elseStore, null, storeChanged);
  }

  /**
   * Create a new {@link #ConditionalTransferResult(AbstractValue, Store, Store, Map, boolean)},
   * using {@code false} for whether the store changed and {@code null} for {@link
   * #exceptionalStores}.
   *
   * @param value the abstract value produced by the transfer function
   * @param thenStore {@link #thenStore}
   * @param elseStore {@link #elseStore}
   * @see #ConditionalTransferResult(AbstractValue, Store, Store, Map, boolean)
   */
  public ConditionalTransferResult(@Nullable V value, S thenStore, S elseStore) {
    this(value, thenStore, elseStore, false);
  }

  /**
   * Create a new {@link #ConditionalTransferResult(AbstractValue, Store, Store, Map, boolean)},
   * using {@code false} for the {@code storeChanged} formal parameter.
   *
   * @param value the abstract value produced by the transfer function
   * @param thenStore {@link #thenStore}
   * @param elseStore {@link #elseStore}
   * @param exceptionalStores {@link #exceptionalStores}
   * @see #ConditionalTransferResult(AbstractValue, Store, Store, Map, boolean)
   */
  public ConditionalTransferResult(
      V value, S thenStore, S elseStore, Map<TypeMirror, S> exceptionalStores) {
    this(value, thenStore, elseStore, exceptionalStores, false);
  }

  /**
   * Create a {@code ConditionalTransferResult} with {@code thenStore} as the resulting store if the
   * corresponding {@link org.checkerframework.dataflow.cfg.node.Node} evaluates to {@code true} and
   * {@code elseStore} otherwise.
   *
   * <p><em>Exceptions</em>: If the corresponding {@link
   * org.checkerframework.dataflow.cfg.node.Node} throws an exception, then the corresponding store
   * in {@code exceptionalStores} is used. If no exception is found in {@code exceptionalStores},
   * then it is assumed that no special handling is necessary and the store before the corresponding
   * {@link org.checkerframework.dataflow.cfg.node.Node} will be passed along any exceptional edge.
   *
   * <p><em>Aliasing</em>: {@code thenStore}, {@code elseStore}, and any store in {@code
   * exceptionalStores} are not allowed to be used anywhere outside of this class (including use
   * through aliases). Complete control over the objects is transferred to this class.
   *
   * @param value the abstract value produced by the transfer function
   * @param thenStore {@link #thenStore}
   * @param elseStore {@link #elseStore}
   * @param exceptionalStores {@link #exceptionalStores}
   * @param storeChanged whether the store changed; see {@link
   *     org.checkerframework.dataflow.analysis.TransferResult#storeChanged}.
   */
  public ConditionalTransferResult(
      @Nullable V value,
      S thenStore,
      S elseStore,
      @Nullable Map<TypeMirror, S> exceptionalStores,
      boolean storeChanged) {
    super(value, exceptionalStores);
    this.thenStore = thenStore;
    this.elseStore = elseStore;
    this.storeChanged = storeChanged;
  }

  /** The regular result store. */
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

  @Override
  public boolean containsTwoStores() {
    return true;
  }

  @Override
  public String toString() {
    StringJoiner result = new StringJoiner(System.lineSeparator());
    result.add("RegularTransferResult(");
    result.add("  resultValue = " + StringsPlume.indentLinesExceptFirst(2, resultValue));
    result.add("  thenStore = " + StringsPlume.indentLinesExceptFirst(2, thenStore));
    result.add("  elseStore = " + StringsPlume.indentLinesExceptFirst(2, elseStore));
    result.add(")");
    return result.toString();
  }

  @Override
  public boolean storeChanged() {
    return storeChanged;
  }
}
