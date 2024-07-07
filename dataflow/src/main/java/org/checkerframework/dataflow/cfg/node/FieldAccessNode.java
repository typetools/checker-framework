package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for a field access, including a method accesses:
 *
 * <pre>
 *   <em>expression</em> . <em>field</em>
 * </pre>
 */
public class FieldAccessNode extends Node {
  /** The tree of the field access. */
  protected final Tree tree;

  /** The element of the accessed field. */
  protected final VariableElement element;

  /** The name of the accessed field. */
  protected final String field;

  /** The receiver node of the field access. */
  protected final Node receiver;

  /**
   * Creates a new FieldAccessNode.
   *
   * @param tree the tree from which to create a FieldAccessNode
   * @param receiver the receiver for the resulting FieldAccessNode
   */
  public FieldAccessNode(Tree tree, Node receiver) {
    super(TreeUtils.typeOf(tree));
    assert TreeUtils.isFieldAccess(tree);
    this.tree = tree;
    this.receiver = receiver;
    this.field = TreeUtils.getFieldName(tree);

    if (tree instanceof MemberSelectTree) {
      MemberSelectTree mstree = (MemberSelectTree) tree;
      assert TreeUtils.isUseOfElement(mstree) : "@AssumeAssertion(nullness): tree kind";
      this.element = TreeUtils.variableElementFromUse(mstree);
    } else if (tree instanceof IdentifierTree) {
      IdentifierTree itree = (IdentifierTree) tree;
      assert TreeUtils.isUseOfElement(itree) : "@AssumeAssertion(nullness): tree kind";
      this.element = TreeUtils.variableElementFromUse(itree);
    } else {
      throw new BugInCF("unexpected tree %s [%s]", tree, tree.getClass());
    }
  }

  public FieldAccessNode(Tree tree, VariableElement element, Node receiver) {
    super(element.asType());
    this.tree = tree;
    this.element = element;
    this.receiver = receiver;
    this.field = element.getSimpleName().toString();
  }

  public VariableElement getElement() {
    return element;
  }

  public Node getReceiver() {
    return receiver;
  }

  public String getFieldName() {
    return field;
  }

  @Override
  public Tree getTree() {
    return tree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitFieldAccess(this, p);
  }

  @Override
  public String toString() {
    if (Node.disambiguateOwner) {
      return getReceiver() + "." + field + "{owner=" + ((Symbol) element).owner + "}";
    } else {
      return getReceiver() + "." + field;
    }
  }

  /**
   * Determine whether the field is static or not.
   *
   * @return whether the field is static or not
   */
  public boolean isStatic() {
    return ElementUtils.isStatic(getElement());
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof FieldAccessNode)) {
      return false;
    }
    FieldAccessNode other = (FieldAccessNode) obj;
    return getReceiver().equals(other.getReceiver()) && getFieldName().equals(other.getFieldName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getReceiver(), getFieldName());
  }

  @Override
  @SideEffectFree
  public Collection<Node> getOperands() {
    return Collections.singletonList(receiver);
  }
}
