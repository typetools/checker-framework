package org.checkerframework.dataflow.cfg.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.Node;

/** Implementation of a regular basic block. */
public class RegularBlockImpl extends SingleSuccessorBlockImpl implements RegularBlock {

  /** Internal representation of the contents. */
  protected final List<Node> contents;

  /**
   * Initialize an empty basic block to be filled with contents and linked to other basic blocks
   * later.
   */
  public RegularBlockImpl() {
    super(BlockType.REGULAR_BLOCK);
    contents = new ArrayList<>();
    long uid = getUid();
    if (uid == 164) {
      new Error("created block " + getUid()).printStackTrace();
    }
  }

  /**
   * Add a node to the contents of this basic block.
   *
   * @param n a node to add to this basic block
   */
  public void addNode(Node n) {
    contents.add(n);
    n.setBlock(this);
  }

  /**
   * Add multiple nodes to the contents of this basic block.
   *
   * @param ns the nodes to add to this basic block
   */
  public void addNodes(List<? extends Node> ns) {
    for (Node n : ns) {
      addNode(n);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns an non-empty list.
   */
  @Override
  public List<Node> getNodes() {
    return Collections.unmodifiableList(contents);
  }

  @Override
  public @Nullable Node getLastNode() {
    return contents.get(contents.size() - 1);
  }

  @Override
  public @Nullable BlockImpl getRegularSuccessor() {
    return successor;
  }

  @Override
  public String toString() {
    return "RegularBlock(" + contents + ")";
  }

  @Override
  public boolean isEmpty() {
    return contents.isEmpty();
  }
}
