package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This represents a resource declaration in a try-with-resources tree. A resource declaration can
 * be a variable declaration, a use of a final (or effectively final) local variable, or a use of a
 * final field.
 */
public class ResourceNode extends Node {

  /**
   * The {@link Node} for the resource declaration, which must be an {@link AssignmentNode}, a
   * {@link LocalVariableNode}, or a {@link FieldAccessNode}
   */
  private final Node resourceDeclarationNode;

  /** The tree for the resource declaration */
  private final Tree resourceTree;

  /**
   * Construct a {@code ResourceNode}
   *
   * @param resourceDeclarationNode the node for the resource declaration
   * @param resourceTree the tree for the resource declaration
   */
  public ResourceNode(Node resourceDeclarationNode, Tree resourceTree) {
    super(resourceDeclarationNode.getType());
    assert resourceDeclarationNode instanceof AssignmentNode
            || resourceDeclarationNode instanceof LocalVariableNode
            || resourceDeclarationNode instanceof FieldAccessNode
        : resourceDeclarationNode.getClass();
    this.resourceDeclarationNode = resourceDeclarationNode;
    this.resourceTree = resourceTree;
  }

  /**
   * Returns the node for the resource declaration, which is guaranteed to be an {@link
   * AssignmentNode}, a {@link LocalVariableNode}, or a {@link FieldAccessNode}.
   *
   * @return the node for the resource declaration.
   */
  public Node getResourceDeclarationNode() {
    return resourceDeclarationNode;
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
    return Collections.singleton(resourceDeclarationNode);
  }

  @Override
  public String toString() {
    return "ResourceNode{"
        + "resourceDeclarationNode="
        + resourceDeclarationNode
        + ", resourceTree="
        + resourceTree
        + '}';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ResourceNode that = (ResourceNode) o;
    return resourceDeclarationNode.equals(that.resourceDeclarationNode)
        && resourceTree.equals(that.resourceTree);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resourceDeclarationNode, resourceTree);
  }
}
