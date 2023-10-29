package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This represents a resource declaration in a try-with-resources tree. A resource declaration can
 * be either a variable declaration or just a final (or effectively final) local variable.
 */
public class ResourceNode extends Node {

  /**
   * The {@link Node} for the resource declaration, which must be either an {@link AssignmentNode}
   * or a {@link LocalVariableNode}
   */
  private final Node assignOrLocalVarNode;

  /** The tree for the resource declaration */
  private final Tree resourceTree;

  /**
   * Construct a {@code ResourceNode}
   *
   * @param assignOrLocalVarNode the node for the resource declaration
   * @param resourceTree the tree for the resource declaration
   */
  public ResourceNode(Node assignOrLocalVarNode, Tree resourceTree) {
    super(assignOrLocalVarNode.getType());
    assert assignOrLocalVarNode instanceof AssignmentNode
            || assignOrLocalVarNode instanceof LocalVariableNode
        : assignOrLocalVarNode.getClass();
    this.assignOrLocalVarNode = assignOrLocalVarNode;
    this.resourceTree = resourceTree;
  }

  /**
   * Returns the node for the resource declaration, which is guaranteed to be either an {@link
   * AssignmentNode} or a {@link LocalVariableNode}.
   *
   * @return the node for the resource declaration.
   */
  public Node getAssignOrLocalVarNode() {
    return assignOrLocalVarNode;
  }

  @Override
  public Tree getTree() {
    return resourceTree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitResource(this, p);
  }

  @Override
  public Collection<Node> getOperands() {
    // TODO what is the right answer here?
    return Collections.emptyList();
  }

  @Override
  public String toString() {
    return "ResourceNode{"
        + "assignOrLocalVarNode="
        + assignOrLocalVarNode
        + ", resourceTree="
        + resourceTree
        + '}';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ResourceNode that = (ResourceNode) o;
    return assignOrLocalVarNode.equals(that.assignOrLocalVarNode)
        && resourceTree.equals(that.resourceTree);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assignOrLocalVarNode, resourceTree);
  }
}
