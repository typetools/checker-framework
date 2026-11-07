package org.checkerframework.dataflow.expression;

import com.sun.source.tree.Tree;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Represents a collection element that is iterated over in a potentially
 * collection-obligation-fulfilling loop, for example {@code o} in {@code for (Object o: list) { }}.
 *
 * <p>An IteratedCollectionElement is identified by its {@link #tree}, compared by reference. Each
 * loop has its own tree, so two loops have different elements even if their code is identical.
 */
public class IteratedCollectionElement extends JavaExpression {
  /**
   * The CFG node for this collection element, such as the {@link LocalVariableNode} for {@code o}
   * in {@code for (Object o : list)}, or the {@code MethodInvocationNode} for {@code list.get(i)}
   * in {@code for (int i = 0; i < list.size(); i++) { list.get(i).close(); }}.
   */
  public final Node node;

  /**
   * The AST tree for this collection element: the tree of {@link #node}, such as the declaration
   * {@code Object o} in {@code for (Object o : list)}, or the invocation {@code list.get(i)} in an
   * indexed loop.
   */
  public final Tree tree;

  /**
   * The expression that refers to this collection element in the loop body, such as the local
   * variable {@code o} or the method call {@code list.get(i)}.
   */
  public final JavaExpression reference;

  /**
   * Creates a new IteratedCollectionElement.
   *
   * @param node the CFG node for a collection element, such as the {@link LocalVariableNode} for
   *     {@code o} in {@code for (Object o : list)}
   * @param tree the tree of {@code node}, such as the declaration {@code Object o}
   */
  public IteratedCollectionElement(Node node, Tree tree) {
    super(node.getType());
    this.node = node;
    this.tree = tree;
    this.reference = JavaExpression.fromNode(node);
  }

  /**
   * Returns true if the given node denotes this collection element: its tree is this element's
   * tree, or its expression equals this element's {@link #reference}. That reference is the local
   * variable that this element's tree declares, such as {@code o} in {@code o.close()} in the body
   * of {@code for (Object o : list)}, or a method call such as {@code list.get(i)} in {@code
   * list.get(i).close()} in the body of {@code for (int i = 0; i < list.size(); i++)}.
   *
   * <p>This method is unsound for a reference that is a nondeterministic method call, such as
   * {@code it.next()} in a while loop over an iterator {@code it}. It returns true for every call
   * {@code it.next()} in the loop body, but each such call yields a different collection element.
   *
   * @param n a CFG node
   * @return true if {@code n} denotes this collection element
   */
  public boolean isDenotedBy(Node n) {
    // `Tree` does not override `equals`, so this compares trees by reference.
    if (tree.equals(n.getTree())) {
      return true;
    }
    // A local variable is a distinct element (VarSymbol) in each loop, so this is not confused by
    // two loops that use the same variable name.  Likewise for a method call such as
    // `list.get(i)`, whose index variable is distinct in each loop.
    // TODO: This is unsound for a nondeterministic method call such as `it.next()`, as documented
    // above.  Matching only deterministic calls would require an AnnotationProvider.
    return (reference instanceof LocalVariable || reference instanceof MethodCall)
        && reference.equals(JavaExpression.fromNode(n));
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof IteratedCollectionElement other)) {
      return false;
    }
    // `Tree` does not override `equals`, so this compares trees by reference.
    return this.tree.equals(other.tree);
  }

  @Override
  public int hashCode() {
    return tree.hashCode();
  }

  @SuppressWarnings("unchecked") // generic cast
  @Override
  public <T extends JavaExpression> @Nullable T containedOfClass(Class<T> clazz) {
    return getClass() == clazz ? (T) this : null;
  }

  @Override
  public String toString() {
    return "IteratedCollectionElement(" + tree + ")";
  }

  @Override
  public boolean isDeterministic(AnnotationProvider provider) {
    return true;
  }

  @Override
  public boolean syntacticEquals(JavaExpression je) {
    if (!(je instanceof IteratedCollectionElement other)) {
      return false;
    }
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
    // Like a local variable, the element cannot be reassigned by other code, but if its type is
    // mutable, other code can mutate it via an alias.
    return !TypesUtils.isImmutableTypeInJdk(type);
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitIteratedCollectionElement(this, p);
  }
}
