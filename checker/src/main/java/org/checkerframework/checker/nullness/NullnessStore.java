package org.checkerframework.checker.nullness;

import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.initialization.InitializationStore;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.plumelib.util.UniqueId;

/**
 * Behaves like {@link InitializationStore}, but additionally tracks whether {@link PolyNull} is
 * known to be {@link NonNull} or {@link Nullable} (or not known to be either).
 */
public class NullnessStore extends InitializationStore<NullnessValue, NullnessStore>
    implements UniqueId {

  /** True if, at this point, {@link PolyNull} is known to be {@link NonNull}. */
  protected boolean isPolyNullNonNull;

  /** True if, at this point, {@link PolyNull} is known to be {@link Nullable}. */
  protected boolean isPolyNullNull;

  /** The unique ID for the next-created object. */
  static final AtomicLong nextUid = new AtomicLong(0);
  /** The unique ID of this object. */
  final long uid = nextUid.getAndIncrement();
  /**
   * Returns the unique ID of this object.
   *
   * @return the unique ID of this object
   */
  @Override
  public long getUid() {
    return uid;
  }

  /**
   * Create a NullnessStore.
   *
   * @param analysis the analysis class this store belongs to
   * @param sequentialSemantics should the analysis use sequential Java semantics (i.e., assume that
   *     only one thread is running at all times)?
   */
  public NullnessStore(
      CFAbstractAnalysis<NullnessValue, NullnessStore, ?> analysis, boolean sequentialSemantics) {
    super(analysis, sequentialSemantics);
    isPolyNullNonNull = false;
    isPolyNullNull = false;
  }

  /**
   * Create a NullnessStore (copy constructor).
   *
   * @param s a store to copy
   */
  public NullnessStore(NullnessStore s) {
    super(s);
    isPolyNullNonNull = s.isPolyNullNonNull;
    isPolyNullNull = s.isPolyNullNull;
  }

  @Override
  public NullnessStore leastUpperBound(NullnessStore other) {
    NullnessStore lub = super.leastUpperBound(other);
    lub.isPolyNullNonNull = isPolyNullNonNull && other.isPolyNullNonNull;
    lub.isPolyNullNull = isPolyNullNull && other.isPolyNullNull;
    return lub;
  }

  @Override
  protected boolean supersetOf(CFAbstractStore<NullnessValue, NullnessStore> o) {
    if (!(o instanceof InitializationStore)) {
      return false;
    }
    NullnessStore other = (NullnessStore) o;
    if ((other.isPolyNullNonNull != isPolyNullNonNull)
        || (other.isPolyNullNull != isPolyNullNull)) {
      return false;
    }
    return super.supersetOf(other);
  }

  @Override
  protected String internalVisualize(CFGVisualizer<NullnessValue, NullnessStore, ?> viz) {
    return super.internalVisualize(viz)
        + viz.getSeparator()
        + viz.visualizeStoreKeyVal("isPolyNullNonNull", isPolyNullNonNull)
        + viz.getSeparator()
        + viz.visualizeStoreKeyVal("isPolyNullNull", isPolyNullNull);
  }

  /**
   * Returns true if, at this point, {@link PolyNull} is known to be {@link NonNull}.
   *
   * @return true if, at this point, {@link PolyNull} is known to be {@link NonNull}
   */
  public boolean isPolyNullNonNull() {
    return isPolyNullNonNull;
  }

  /**
   * Set the value of whether, at this point, {@link PolyNull} is known to be {@link NonNull}.
   *
   * @param isPolyNullNonNull whether, at this point, {@link PolyNull} is known to be {@link
   *     NonNull}
   */
  public void setPolyNullNonNull(boolean isPolyNullNonNull) {
    this.isPolyNullNonNull = isPolyNullNonNull;
  }

  /**
   * Returns true if, at this point, {@link PolyNull} is known to be {@link Nullable}.
   *
   * @return true if, at this point, {@link PolyNull} is known to be {@link Nullable}
   */
  public boolean isPolyNullNull() {
    return isPolyNullNull;
  }

  /**
   * Set the value of whether, at this point, {@link PolyNull} is known to be {@link Nullable}.
   *
   * @param isPolyNullNull whether, at this point, {@link PolyNull} is known to be {@link Nullable}
   */
  public void setPolyNullNull(boolean isPolyNullNull) {
    this.isPolyNullNull = isPolyNullNull;
  }
}
