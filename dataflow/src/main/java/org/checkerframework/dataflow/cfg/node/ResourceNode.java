package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class ResourceNode extends Node {

  private final Node declOrLocalVarNode;

  private final Tree resourceTree;

  public ResourceNode(Node declOrLocalVarNode, Tree resourceTree) {
    super(declOrLocalVarNode.getType());
    this.declOrLocalVarNode = declOrLocalVarNode;
    this.resourceTree = resourceTree;
  }

  public Node getDeclOrLocalVarNode() {
    return declOrLocalVarNode;
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
        + "declOrIdentifierNode="
        + declOrLocalVarNode
        + ", resourceTree="
        + resourceTree
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ResourceNode that = (ResourceNode) o;
    return declOrLocalVarNode.equals(that.declOrLocalVarNode)
        && resourceTree.equals(that.resourceTree);
  }

  @Override
  public int hashCode() {
    return Objects.hash(declOrLocalVarNode, resourceTree);
  }
}
