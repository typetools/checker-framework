package org.checkerframework.framework.type.visitor;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.BugInCF;

/**
 * An {@link AnnotatedTypeScanner} that scans an {@link AnnotatedTypeMirror} and performs some
 * {@link #defaultAction} on each type. The defaultAction can be passed to the constructor {@link
 * #SimpleAnnotatedTypeScanner(DefaultAction)} or this class can be extended and {@link
 * #defaultAction} can be overridden.
 *
 * <p>If the default action does not return a result, then {@code R} should be {@link Void}. If the
 * default action returns a result, then specify a {@link #reduce} function.
 *
 * @param <R> the return type of this visitor's methods. Use Void for visitors that do not need to
 *     return results.
 * @param <P> the type of the additional parameter to this visitor's methods. Use Void for visitors
 *     that do not need an additional parameter.
 */
public class SimpleAnnotatedTypeScanner<R, P> extends AnnotatedTypeScanner<R, P> {

  /**
   * Represents an action to perform on every type.
   *
   * @param <R> the type of the result of the action
   * @param <P> the type of the parameter of action
   */
  @FunctionalInterface
  public interface DefaultAction<R, P> {

    /**
     * The action to perform on every type.
     *
     * @param type AnnotatedTypeMirror on which to perform some action
     * @param p argument to pass to the action
     * @return result of the action
     */
    R defaultAction(AnnotatedTypeMirror type, P p);
  }

  /** The action to perform on every type. */
  protected final DefaultAction<R, P> defaultAction;

  /**
   * Creates a scanner that performs {@code defaultAction} on every type.
   *
   * <p>Use this constructor if the type of result of the default action is {@link Void}.
   *
   * @param defaultAction action to perform on every type
   */
  public SimpleAnnotatedTypeScanner(DefaultAction<R, P> defaultAction) {
    this(defaultAction, null, null);
  }

  /**
   * Creates a scanner that performs {@code defaultAction} on every type and use {@code reduce} to
   * combine the results.
   *
   * <p>Use this constructor if the default action returns a result.
   *
   * @param defaultAction action to perform on every type
   * @param reduce function used to combine results
   * @param defaultResult result to use by default
   */
  public SimpleAnnotatedTypeScanner(
      DefaultAction<R, P> defaultAction, Reduce<R> reduce, R defaultResult) {
    super(reduce, defaultResult);
    this.defaultAction = defaultAction;
  }

  /**
   * Creates a scanner without specifing the default action. Subclasses may only use this
   * constructor if they also override {@link #defaultAction(AnnotatedTypeMirror, Object)}.
   */
  protected SimpleAnnotatedTypeScanner() {
    this(null, null, null);
  }

  /**
   * Creates a scanner without specifing the default action. Subclasses may only use this
   * constructor if they also override {@link #defaultAction(AnnotatedTypeMirror, Object)}.
   *
   * @param reduce function used to combine results
   * @param defaultResult result to use by default
   */
  protected SimpleAnnotatedTypeScanner(Reduce<R> reduce, R defaultResult) {
    this(null, reduce, defaultResult);
  }

  /**
   * Called by default for any visit method that is not overridden.
   *
   * @param type the type to visit
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  protected R defaultAction(AnnotatedTypeMirror type, P p) {
    if (defaultAction == null) {
      // The no argument constructor sets default action to null.
      throw new BugInCF(
          "%s did not provide a default action.  Please override #defaultAction", this.getClass());
    }
    return defaultAction.defaultAction(type, p);
  }

  /**
   * Visits a declared type.
   *
   * @param type the type to visit
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  @Override
  public final R visitDeclared(AnnotatedDeclaredType type, P p) {
    R r = defaultAction(type, p);
    return reduce(super.visitDeclared(type, p), r);
  }

  /**
   * Visits an executable type.
   *
   * @param type the type to visit
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  @Override
  public final R visitExecutable(AnnotatedExecutableType type, P p) {
    R r = defaultAction(type, p);
    return reduce(super.visitExecutable(type, p), r);
  }

  /**
   * Visits an array type.
   *
   * @param type the type to visit
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  @Override
  public final R visitArray(AnnotatedArrayType type, P p) {
    R r = defaultAction(type, p);
    return reduce(super.visitArray(type, p), r);
  }

  /**
   * Visits a type variable.
   *
   * @param type the type to visit
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  @Override
  public final R visitTypeVariable(AnnotatedTypeVariable type, P p) {
    R r = defaultAction(type, p);
    return reduce(super.visitTypeVariable(type, p), r);
  }

  /**
   * Visits a primitive type.
   *
   * @param type the type to visit
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  @Override
  public final R visitPrimitive(AnnotatedPrimitiveType type, P p) {
    return defaultAction(type, p);
  }

  /**
   * Visits NoType type.
   *
   * @param type the type to visit
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  @Override
  public final R visitNoType(AnnotatedNoType type, P p) {
    return defaultAction(type, p);
  }

  /**
   * Visits a {@code null} type.
   *
   * @param type the type to visit
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  @Override
  public final R visitNull(AnnotatedNullType type, P p) {
    return defaultAction(type, p);
  }

  /**
   * Visits a wildcard type.
   *
   * @param type the type to visit
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  @Override
  public final R visitWildcard(AnnotatedWildcardType type, P p) {
    R r = defaultAction(type, p);
    return reduce(super.visitWildcard(type, p), r);
  }
}
