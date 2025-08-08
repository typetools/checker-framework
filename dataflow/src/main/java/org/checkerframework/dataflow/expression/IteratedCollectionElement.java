package org.checkerframework.dataflow.expression;

import com.sun.source.tree.Tree;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.AnnotationProvider;

/**
 * Represents a collection element that is iterated over in a potentially
 * collection-obligation-fulfilling loop, for example {@code o} in {@code for (Object o: list) { }}.
 */
public class IteratedCollectionElement extends JavaExpression {
  /** The CFG node for this collection element. */
  public final Node node;

  /** The AST node for this collection element. */
  public final Tree tree;

  /**
   * Creates a new IteratedCollectionElement.
   *
   * @param var a CFG node
   * @param tree an AST tree
   */
  public IteratedCollectionElement(Node var, Tree tree) {
    super(var.getType());
    this.node = var;
    this.tree = tree;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof IteratedCollectionElement)) {
      return false;
    }
    IteratedCollectionElement other = (IteratedCollectionElement) obj;
    return other.tree.equals(this.tree) && other.node.equals(this.node);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tree, node);
  }

  @SuppressWarnings("unchecked") // generic cast
  @Override
  public <T extends JavaExpression> @Nullable T containedOfClass(Class<T> clazz) {
    return getClass() == clazz ? (T) this : null;
  }

  @Override
  public boolean isDeterministic(AnnotationProvider provider) {
    return true;
  }

  @Override
  public boolean syntacticEquals(JavaExpression je) {
    if (!(je instanceof IteratedCollectionElement)) {
      return false;
    }
    IteratedCollectionElement other = (IteratedCollectionElement) je;
    return this.equals(other);
  }

  @Override
  public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
    return syntacticEquals(other);
  }

  @Override
  public boolean isAssignableByOtherCode() {
    return false;
  }

  @Override
  public boolean isModifiableByOtherCode() {
    return false;
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitIteratedCollectionElement(this, p);
  }
}
