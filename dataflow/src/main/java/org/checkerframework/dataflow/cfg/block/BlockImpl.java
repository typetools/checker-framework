package org.checkerframework.dataflow.cfg.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.plumelib.util.ArraySet;

/** Base class of the {@link Block} implementation hierarchy. */
public abstract class BlockImpl implements Block {

  /** The type of this basic block. */
  protected final BlockType type;

  /** The set of predecessors. */
  protected final Set<BlockImpl> predecessors;

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
  public long getUid(@UnknownInitialization BlockImpl this) {
    return uid;
  }

  /**
   * Create a new BlockImpl.
   *
   * @param type the type of this basic block
   */
  protected BlockImpl(BlockType type) {
    this.type = type;
    // Most blocks have few predecessors
    this.predecessors = new ArraySet<>(2);
  }

  @Override
  public BlockType getType() {
    return type;
  }

  @Override
  public List<Block> getPredecessors() {
    // Not "Collections.unmodifiableSet(predecessors)" which has nondeterministic iteration
    // order.
    return new ArrayList<>(predecessors);
  }

  public void addPredecessor(BlockImpl pred) {
    predecessors.add(pred);
  }

  public void removePredecessor(BlockImpl pred) {
    predecessors.remove(pred);
  }
}
