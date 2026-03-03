package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node representing a class name used in an expression such as a static method invocation.
 *
 * <p>parent.<em>class</em> .forName(...)
 */
public class ClassNameNode extends Node {

  /** The tree for this node. */
  protected final @Nullable Tree tree;

  /** The class named by this node. Either a TypeElement or a TypeParameterElement. */
  protected final Element element;

  /** The parent name, if any. */
  protected final @Nullable Node parent;

  /**
   * Creates a new ClassNameNode.
   *
   * @param tree the tree for the new node
   */
  public ClassNameNode(IdentifierTree tree) {
    super(TreeUtils.typeOf(tree));
    this.tree = tree;
    assert TreeUtils.isUseOfElement(tree) : "@AssumeAssertion(nullness): tree kind";
    Element element = TreeUtils.elementFromUse(tree);
    assert element instanceof TypeElement || element instanceof TypeParameterElement
        : "@AssumeAssertion(nullness)";
    this.element = element;
    this.parent = null;
  }

  /**
   * Create a new ClassNameNode.
   *
   * @param tree the class tree for this node
   */
  public ClassNameNode(ClassTree tree) {
    super(TreeUtils.typeOf(tree));
    this.tree = tree;
    Element element = TreeUtils.elementFromDeclaration(tree);
    assert element instanceof TypeElement || element instanceof TypeParameterElement
        : "@AssumeAssertion(nullness)";
    this.element = element;
    this.parent = null;
  }

  public ClassNameNode(MemberSelectTree tree, Node parent) {
    super(TreeUtils.typeOf(tree));
    this.tree = tree;
    assert TreeUtils.isUseOfElement(tree) : "@AssumeAssertion(nullness): tree kind";
    Element element = TreeUtils.elementFromUse(tree);
    assert element instanceof TypeElement || element instanceof TypeParameterElement
        : "@AssumeAssertion(nullness)";
    this.element = element;
    this.parent = parent;
  }

  public ClassNameNode(TypeMirror type, Element element) {
    super(type);
    this.tree = null;
    this.element = element;
    assert element instanceof TypeElement || element instanceof TypeParameterElement;
    this.parent = null;
  }

  public Element getElement() {
    return element;
  }

  /** The parent node of the current node. */
  public @Nullable Node getParent() {
    return parent;
  }

  @Override
  public @Nullable Tree getTree() {
    return tree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitClassName(this, p);
  }

  @Override
  public String toString() {
    return getElement().getSimpleName().toString();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof ClassNameNode)) {
      return false;
    }
    ClassNameNode other = (ClassNameNode) obj;
    return Objects.equals(getParent(), other.getParent())
        && getElement().equals(other.getElement());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getElement(), getParent());
  }

  @Override
  @SideEffectFree
  public Collection<Node> getOperands() {
    if (parent == null) {
      return Collections.emptyList();
    }
    return Collections.singleton(parent);
  }
}
