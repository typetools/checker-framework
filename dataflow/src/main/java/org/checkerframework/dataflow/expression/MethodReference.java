package org.checkerframework.dataflow.expression;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.AnnotationProvider;

/**
 * A <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.13">Java Method
 * Reference expression</a>
 */
public class MethodReference extends JavaExpression {

  /** The scope of this method reference. */
  protected final MethodReferenceScope scope;

  /** The target of this method reference. */
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
    T result = containedOfClassForScope(clazz);
    return result != null ? result : containedOfClassForTarget(clazz);
  }

  /**
   * Return the first subexpression in {@link scope} whose class is the given class, or null.
   *
   * @param clazz the class
   * @return the first subexpression in {@link scope} whose class is the given class, or null
   * @param <T> the class
   */
  private <T extends JavaExpression> @Nullable T containedOfClassForScope(Class<T> clazz) {
    if (scope.isScopeSuper()) {
      return null;
    }
    T result = null;
    if (scope.getExpression() != null) {
      result = scope.getExpression().containedOfClass(clazz);
      if (result != null) {
        return result;
      }
    }
    if (scope.getType() != null) {
      result = scope.getType().containedOfClass(clazz);
    }
    return result;
  }

  /**
   * Return the first subexpression in {@link target} whose class is the given class, or null.
   *
   * @param clazz the class
   * @return the first subexpression in {@link target} whose class is the given class, or null
   * @param <T> the class
   */
  private <T extends JavaExpression> @Nullable T containedOfClassForTarget(Class<T> clazz) {
    T result = null;
    if (target.getTypeArguments() != null) {
      result = target.getTypeArguments().containedOfClass(clazz);
      if (result != null) {
        return result;
      }
    }
    if (target.getIdentifier() != null) {
      result = target.getIdentifier().containedOfClass(clazz);
    }
    return result;
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
    return null;
  }

  @Override
  public String toString() {
    return this.scope.toString() + "::" + this.target.toString();
  }
}
