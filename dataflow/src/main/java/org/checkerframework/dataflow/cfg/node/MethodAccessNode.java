package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for a method access, including a receiver:
 *
 * <pre>
 *   <em>expression</em> . <em>method</em> ()
 * </pre>
 */
public class MethodAccessNode extends Node {

  protected final ExpressionTree tree;
  protected final ExecutableElement method;
  protected final Node receiver;

  // TODO: add method to get modifiers (static, access level, ..)

  public MethodAccessNode(ExpressionTree tree, Node receiver) {
    super(TreeUtils.typeOf(tree));
    assert TreeUtils.isMethodAccess(tree);
    this.tree = tree;
    assert TreeUtils.isUseOfElement(tree) : "@AssumeAssertion(nullness): tree kind";
    this.method = (ExecutableElement) TreeUtils.elementFromUse(tree);
    this.receiver = receiver;
  }

  public ExecutableElement getMethod() {
    return method;
  }

  public Node getReceiver() {
    return receiver;
  }

  @Override
  public Tree getTree() {
    return tree;
  }

  /**
   * Returns the owner of the method -- that is, the class that defines it.
   *
   * @return the owner of the method -- that is, the class that defines it
   */
  public TypeElement getOwner() {
    return (TypeElement) ((MethodSymbol) method).owner;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitMethodAccess(this, p);
  }

  @Override
  public String toString() {
    return getReceiver() + "." + method.getSimpleName();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof MethodAccessNode)) {
      return false;
    }
    MethodAccessNode other = (MethodAccessNode) obj;
    return getReceiver().equals(other.getReceiver()) && getMethod().equals(other.getMethod());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getReceiver(), getMethod());
  }

  @Override
  public Collection<Node> getOperands() {
    return Collections.singletonList(receiver);
  }
}
