package org.checkerframework.dataflow.expression;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.AnnotationProvider;

/**
 * A <a href="https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.13">Java Method
 * Reference expression</a>.
 */
public class MethodReference extends JavaExpression {

  /** The scope of this method reference, which precedes "::". */
  protected final MethodReferenceScope scope;

  /** The target of this method reference, which follows "::". */
  protected final MethodReferenceTarget target;

  /**
   * Creates a new method reference.
   *
   * @param type the type of this method reference
   * @param scope the scope of this method reference
   * @param target the target of this method reference
   */
  public MethodReference(
      TypeMirror type, MethodReferenceScope scope, MethodReferenceTarget target) {
    super(type);
    this.scope = scope;
    this.target = target;
  }

  @Override
  public <T extends JavaExpression> @Nullable T containedOfClass(Class<T> clazz) {
    T result = scope.containedOfClass(clazz);
    return result != null ? result : target.containedOfClass(clazz);
  }

  @Override
  public boolean isDeterministic(AnnotationProvider provider) {
    return false;
  }

  @Override
  public boolean syntacticEquals(JavaExpression je) {
    if (!(je instanceof MethodReference)) {
      return false;
    }
    MethodReference other = (MethodReference) je;
    return scope.equals(other.scope) && target.equals(other.target);
  }

  @Override
  public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
    return false;
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitMethodReference(this, p); // Stub?
  }

  @Override
  public String toString() {
    return this.scope.toString() + "::" + this.target.toString();
  }
}
