package org.checkerframework.dataflow.expression;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.TypesUtils;

/** A use of {@code super}. */
public class SuperReference extends JavaExpression {

  /**
   * Creates a new {@link SuperReference}.
   *
   * @param type the type of the {@code super} reference
   */
  public SuperReference(TypeMirror type) {
    super(type);
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
  public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
    return false;
  }

  @Override
  public boolean syntacticEquals(JavaExpression je) {
    return je instanceof SuperReference;
  }

  @Override
  public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
    return this.syntacticEquals(other);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return obj instanceof SuperReference;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitSuperReference(this, p);
  }

  @Override
  public String toString() {
    return "super";
  }
}
