package org.checkerframework.dataflow.expression;

import com.sun.source.tree.Tree;
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

  @SuppressWarnings("interning:not.interned")
  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof IteratedCollectionElement)) {
      return false;
    }
    IteratedCollectionElement other = (IteratedCollectionElement) obj;
    return other.tree.equals(this.tree) || other.node.equals(this.node);
  }

  // /**
  //  * Returns true if the two elements are the same.
  //  *
  //  * @param element1 the first element to compare
  //  * @param element2 the second element to compare
  //  * @return true if the two elements are the same
  //  */
  // protected static boolean sameElement(VariableElement element1, VariableElement element2) {
  //   VarSymbol vs1 = (VarSymbol) element1;
  //   VarSymbol vs2 = (VarSymbol) element2;
  //   // If a LocalVariable is created via JavaExpressionParseUtil#parse, then `vs1.equals(vs2)`
  //   // will not return true even if the elements represent the same local variable.
  //   // The owner of a lambda parameter is the enclosing method, so a local variable and a lambda
  //   // parameter might have the same name and the same owner. Use pos to differentiate this
  //   // case.
  //   return vs1.pos == vs2.pos && vs1.name == vs2.name && vs1.owner.equals(vs2.owner);
  // }

  // /**
  //  * Returns the element for this variable.
  //  *
  //  * @return the element for this variable
  //  */
  // public VariableElement getElement() {
  //   return element;
  // }

  @Override
  public int hashCode() {
    return node.hashCode();
    // return Objects.hash(tree.hashCode());
  }

  // @Override
  // public String toString() {
  //   return var.toString();
  // }

  // @Override
  // public String toStringDebug() {
  //   return super.toStringDebug() + " [owner=" + ((VarSymbol) element).owner + "]";
  // }

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
    // return !TypesUtils.isImmutableTypeInJdk(((VarSymbol) element).type);
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitIteratedCollectionElement(this, p);
  }
}
