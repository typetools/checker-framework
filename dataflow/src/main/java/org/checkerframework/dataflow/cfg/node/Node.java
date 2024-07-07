package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.builder.CFGBuilder;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.plumelib.util.UniqueId;

/**
 * A node in the abstract representation used for Java code inside a basic block.
 *
 * <p>The following invariants hold:
 *
 * <pre>
 * block == null || block instanceof RegularBlock || block instanceof ExceptionBlock
 * block != null &hArr; block.getNodes().contains(this)
 * </pre>
 *
 * <pre>
 * type != null
 * tree != null &rArr; node.getType() == InternalUtils.typeOf(node.getTree())
 * </pre>
 *
 * Note that two {@code Node}s can be {@code .equals} but represent different CFG nodes. Take care
 * to use reference equality, maps that handle identity {@code IdentityHashMap}, and sets like
 * {@code IdentityArraySet}.
 */
public abstract class Node implements UniqueId {

  /**
   * If true, print the owner of each field and {@code this}, to disambiguate shadowing. This field
   * is intended for debugging.
   */
  public static final boolean disambiguateOwner = false;

  /**
   * The basic block this node belongs to. If null, this object represents a method formal
   * parameter.
   *
   * <p>Is set by {@link #setBlock}.
   */
  protected @Nullable Block block;

  /**
   * Is this node an l-value?
   *
   * <p>Is set by {@link #setLValue}.
   */
  protected boolean lvalue = false;

  /**
   * Does this node represent a tree that appears in the source code (true) or one that the CFG
   * builder added while desugaring (false).
   *
   * <p>Is set by {@link #setInSource}.
   */
  protected boolean inSource = true;

  /**
   * The type of this node. For {@link Node}s with {@link Tree}s, this type is the type of the
   * {@link Tree}. Otherwise, it is the type is set by the {@link CFGBuilder}.
   */
  protected final TypeMirror type;

  /** The unique ID for the next-created object. */
  private static final AtomicLong nextUid = new AtomicLong(0);

  /** The unique ID of this object. */
  private final transient long uid = nextUid.getAndIncrement();

  @Override
  @Pure
  public long getUid(@UnknownInitialization Node this) {
    return uid;
  }

  /**
   * Creates a new Node.
   *
   * @param type the type of the node
   */
  protected Node(TypeMirror type) {
    assert type != null;
    this.type = type;
  }

  /**
   * Returns the basic block this node belongs to (or {@code null} if it represents the parameter of
   * a method).
   *
   * @return the basic block this node belongs to (or {@code null} if it represents the parameter of
   *     a method)
   */
  @Pure
  public @Nullable Block getBlock() {
    return block;
  }

  /** Set the basic block this node belongs to. */
  public void setBlock(Block b) {
    block = b;
  }

  /**
   * Returns the {@link Tree} in the abstract syntax tree, or {@code null} if no corresponding tree
   * exists. For instance, this is the case for an {@link ImplicitThisNode}.
   *
   * @return the corresponding {@link Tree} or {@code null}
   */
  @Pure
  public abstract @Nullable Tree getTree();

  /**
   * Returns a {@link TypeMirror} representing the type of a {@link Node}. A {@link Node} will
   * always have a type even when it has no {@link Tree}.
   *
   * @return a {@link TypeMirror} representing the type of this {@link Node}
   */
  @Pure
  public TypeMirror getType() {
    return type;
  }

  /**
   * Accept method of the visitor pattern.
   *
   * @param <R> result type of the operation
   * @param <P> parameter type
   * @param visitor the visitor to be applied to this node
   * @param p the parameter for this operation
   */
  public abstract <R, P> R accept(NodeVisitor<R, P> visitor, P p);

  /** Is the node an lvalue or not? */
  @Pure
  public boolean isLValue() {
    return lvalue;
  }

  /** Make this node an l-value. */
  public void setLValue() {
    lvalue = true;
  }

  /**
   * Return whether this node represents a tree that appears in the source code (true) or one that
   * the CFG or builder added while desugaring (false).
   *
   * @return whether this node represents a tree that appears in the source code
   */
  @Pure
  public boolean getInSource() {
    return inSource;
  }

  public void setInSource(boolean inSrc) {
    inSource = inSrc;
  }

  /**
   * Returns a collection containing all of the operand {@link Node}s of this {@link Node}.
   *
   * @return a collection containing all of the operand {@link Node}s of this {@link Node}
   */
  @SideEffectFree
  public abstract Collection<Node> getOperands();

  /**
   * Returns a collection containing all of the operand {@link Node}s of this {@link Node}, as well
   * as (transitively) the operands of its operands.
   *
   * @return a collection containing all of the operand {@link Node}s of this {@link Node}, as well
   *     as (transitively) the operands of its operands
   */
  @Pure
  public Collection<Node> getTransitiveOperands() {
    ArrayDeque<Node> operands = new ArrayDeque<>(getOperands());
    ArrayDeque<Node> transitiveOperands = new ArrayDeque<>(operands.size());
    while (!operands.isEmpty()) {
      Node next = operands.removeFirst();
      operands.addAll(next.getOperands());
      transitiveOperands.add(next);
    }
    return transitiveOperands;
  }

  /**
   * Returns a verbose string representation of this, useful for debugging.
   *
   * @return a printed representation of this
   */
  @Pure
  public String toStringDebug() {
    return String.format("%s [%s]", this, this.getClassAndUid());
  }

  /**
   * Returns a verbose string representation of a collection of nodes, useful for debugging..
   *
   * @param nodes a collection of nodes to format
   * @return a printed representation of the given collection
   */
  @Pure
  public static String nodeCollectionToString(Collection<? extends Node> nodes) {
    StringJoiner result = new StringJoiner(", ", "[", "]");
    for (Node n : nodes) {
      result.add(n.toStringDebug());
    }
    return result.toString();
  }
}
