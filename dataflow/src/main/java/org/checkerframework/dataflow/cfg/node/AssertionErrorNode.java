package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * A node for the {@link AssertionError} when an assertion fails or when a method call marked {@link
 * org.checkerframework.dataflow.qual.AssertMethod} fails.
 *
 * <pre>
 *   assert <em>condition</em> : <em>detail</em> ;
 * </pre>
 */
public class AssertionErrorNode extends Node {

  /** Tree for the assert statement or assert method. */
  protected final Tree tree;

  /** The condition that if it is false, the assertion exception is thrown. */
  protected final Node condition;

  /** The node for the expression after {@code :} in the assert statement, or null. */
  protected final @Nullable Node detail;

  /**
   * Creates an AssertionErrorNode.
   *
   * @param tree tree for the assert statement or assert method
   * @param condition the node of the condition when if false the assertion exception is thrown
   * @param detail node for the expression after {@code :} in the assert statement, or null
   * @param type the type of the exception thrown
   */
  public AssertionErrorNode(Tree tree, Node condition, @Nullable Node detail, TypeMirror type) {
    // TODO: Find out the correct "type" for statements.
    // Is it TypeKind.NONE?
    super(type);
    this.tree = tree;
    this.condition = condition;
    this.detail = detail;
  }

  /**
   * The node of the condition that if it is false, the assertion exception is thrown.
   *
   * @return the node of the condition that if it is false, the assertion exception is thrown
   */
  @Pure
  public Node getCondition() {
    return condition;
  }

  /**
   * The node for the expression after {@code :} in the assert statement, or null.
   *
   * @return node for the expression after {@code :} in the assert statement, or null
   */
  @Pure
  public @Nullable Node getDetail() {
    return detail;
  }

  @Override
  public Tree getTree() {
    return tree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitAssertionError(this, p);
  }

  @Override
  public String toString() {
    return "AssertionError(" + getDetail() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof AssertionErrorNode)) {
      return false;
    }
    AssertionErrorNode other = (AssertionErrorNode) obj;
    return Objects.equals(getCondition(), other.getCondition())
        && Objects.equals(getDetail(), other.getDetail());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCondition(), getDetail());
  }

  @Override
  @SideEffectFree
  public Collection<Node> getOperands() {
    if (getDetail() == null) {
      return Collections.singleton(getCondition());
    }
    return Arrays.asList(getCondition(), getDetail());
  }
}
