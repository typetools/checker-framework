package org.checkerframework.dataflow.expression;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.AnnotationProvider;

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

  @Override
  public <T extends JavaExpression> @Nullable T containedOfClass(Class<T> clazz) {
    return null;
  }

  @Override
  public boolean isDeterministic(AnnotationProvider provider) {
    return true;
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
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return null;
  }

  @Override
  public String toString() {
    return "super";
  }
}
