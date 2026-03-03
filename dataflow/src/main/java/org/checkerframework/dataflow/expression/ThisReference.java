package org.checkerframework.dataflow.expression;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.TypesUtils;

/** A use of {@code this}. */
public class ThisReference extends JavaExpression {
  /**
   * Create a new ThisReference.
   *
   * @param type the type of the {@code this} reference
   */
  public ThisReference(TypeMirror type) {
    super(type);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return obj instanceof ThisReference;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    if (Node.disambiguateOwner) {
      return "this{" + type + "}";
    } else {
      return "this";
    }
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
  public boolean isAssignableByOtherCode() {
    return false;
  }

  @Override
  public boolean isModifiableByOtherCode() {
    return !TypesUtils.isImmutableTypeInJdk(type);
  }

  @Override
  public boolean syntacticEquals(JavaExpression je) {
    return je instanceof ThisReference;
  }

  @Override
  public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
    return this.syntacticEquals(other);
  }

  @Override
  public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
    return false; // 'this' is not modifiable
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitThisReference(this, p);
  }
}
