package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.element.Element;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for a local variable or a parameter:
 *
 * <pre>
 *   <em>identifier</em>
 * </pre>
 *
 * We allow local variable uses introduced by the {@link
 * org.checkerframework.dataflow.cfg.builder.CFGBuilder} without corresponding AST {@link Tree}s.
 */
// TODO: don't use for parameters, as they don't have a tree
public class LocalVariableNode extends Node {

  /** The tree for the local variable. */
  protected final Tree tree;

  /** The receiver node for the local variable, {@code null} otherwise. */
  protected final @Nullable Node receiver;

  /**
   * Create a new local variable node for the given tree.
   *
   * @param tree thre tree for the local variable: a VariableTree or an IdentifierTree
   */
  public LocalVariableNode(Tree tree) {
    this(tree, null);
  }

  /**
   * Create a new local variable node for the given tree and receiver.
   *
   * @param tree the tree for the local variable: a VariableTree or an IdentifierTree
   * @param receiver the receiver for the local variable, or null if none
   */
  public LocalVariableNode(Tree tree, @Nullable Node receiver) {
    super(TreeUtils.typeOf(tree));
    // IdentifierTree for normal uses of the local variable or parameter,
    // and VariableTree for declarations or the translation of an initializer block
    assert tree != null;
    assert tree instanceof IdentifierTree || tree instanceof VariableTree;
    this.tree = tree;
    this.receiver = receiver;
  }

  public Element getElement() {
    Element el;
    if (tree instanceof IdentifierTree) {
      IdentifierTree itree = (IdentifierTree) tree;
      assert TreeUtils.isUseOfElement(itree) : "@AssumeAssertion(nullness): tree kind";
      el = TreeUtils.elementFromUse(itree);
    } else {
      assert tree instanceof VariableTree;
      el = TreeUtils.elementFromDeclaration((VariableTree) tree);
    }
    return el;
  }

  /** The receiver node for the local variable, {@code null} otherwise. */
  public @Nullable Node getReceiver() {
    return receiver;
  }

  public String getName() {
    if (tree instanceof IdentifierTree) {
      return ((IdentifierTree) tree).getName().toString();
    }
    return ((VariableTree) tree).getName().toString();
  }

  @Override
  public Tree getTree() {
    return tree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitLocalVariable(this, p);
  }

  @Override
  public String toString() {
    return getName().toString();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof LocalVariableNode)) {
      return false;
    }
    LocalVariableNode other = (LocalVariableNode) obj;
    return getName().equals(other.getName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName());
  }

  @Override
  public Collection<Node> getOperands() {
    return Collections.emptyList();
  }
}
