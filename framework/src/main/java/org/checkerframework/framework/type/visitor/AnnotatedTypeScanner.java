package org.checkerframework.framework.type.visitor;

import java.util.IdentityHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;

/**
 * An {@code AnnotatedTypeScanner} visits an {@link AnnotatedTypeMirror} and all of its child {@link
 * AnnotatedTypeMirror} and preforms some function depending on the kind of type. (By contrast, a
 * {@link SimpleAnnotatedTypeScanner} scans an {@link AnnotatedTypeMirror} and performs the
 * <em>same</em> function regardless of the kind of type.) The function returns some value with type
 * {@code R} and takes an argument of type {@code P}. If the function does not return any value,
 * then {@code R} should be {@link Void}. If the function takes no additional argument, then {@code
 * P} should be {@link Void}.
 *
 * <p>The default implementation of the visitAnnotatedTypeMirror methods will determine a result as
 * follows:
 *
 * <ul>
 *   <li>If the type being visited has no children, the {@link #defaultResult} is returned.
 *   <li>If the type being visited has one child, the result of visiting the child type is returned.
 *   <li>If the type being visited has more than one child, the result is determined by visiting
 *       each child in turn, and then combining the result of each with the cumulative result so
 *       far, as determined by the {@link #reduce} method.
 * </ul>
 *
 * The {@link #reduce} method combines the results of visiting child types. It can be specified by
 * passing a {@link Reduce} object to one of the constructors or by overriding the method directly.
 * If it is not otherwise specified, then reduce returns the first result if it is not null;
 * otherwise, the second result is returned. If the default result is nonnull and reduce never
 * returns null, then both parameters passed to reduce will be nonnull.
 *
 * <p>When overriding a visitAnnotatedTypeMirror method, the returned expression should be {@code
 * reduce(super.visitAnnotatedTypeMirror(type, parameter), result)} so that the whole type is
 * scanned.
 *
 * <p>To begin scanning a type call {@link #visit(AnnotatedTypeMirror, Object)} or (to pass {@code
 * null} as the last parameter) call {@link #visit(AnnotatedTypeMirror)}. Both methods call {@link
 * #reset()}.
 *
 * <p>Here is an example of a scanner that counts the number of {@link AnnotatedTypeVariable} in an
 * AnnotatedTypeMirror.
 *
 * <pre>
 * {@code class CountTypeVariable extends AnnotatedTypeScanner<Integer, Void>} {
 *    public CountTypeVariable() {
 *        super(Integer::sum, 0);
 *    }
 *
 *    {@literal @}Override
 *     public Integer visitTypeVariable(AnnotatedTypeVariable type, Void p) {
 *         return reduce(super.visitTypeVariable(type, p), 1);
 *     }
 * }
 * </pre>
 *
 * An {@code AnnotatedTypeScanner} keeps a map of visited types, in order to prevent infinite
 * recursion on recursive types. Because of this map, you should not create a new {@code
 * AnnotatedTypeScanner} for each use. Instead, store an {@code AnnotatedTypeScanner} as a field in
 * the {@link org.checkerframework.framework.type.AnnotatedTypeFactory} or {@link
 * org.checkerframework.common.basetype.BaseTypeVisitor} of the checker.
 *
 * <p>Below is an example of how to use {@code CountTypeVariable}.
 *
 * <pre>{@code
 * private final CountTypeVariable countTypeVariable = new CountTypeVariable();
 *
 * void method(AnnotatedTypeMirror type) {
 *     int count = countTypeVariable.visit(type);
 * }
 * }</pre>
 *
 * @param <R> the return type of this visitor's methods. Use Void for visitors that do not need to
 *     return results.
 * @param <P> the type of the additional parameter to this visitor's methods. Use Void for visitors
 *     that do not need an additional parameter.
 */
public abstract class AnnotatedTypeScanner<R, P> implements AnnotatedTypeVisitor<R, P> {

  /**
   * Reduces two results into a single result.
   *
   * @param <R> the result type
   */
  @FunctionalInterface
  public interface Reduce<R> {

    /**
     * Returns the combination of two results.
     *
     * @param r1 the first result
     * @param r2 the second result
     * @return the combination of the two results
     */
    R reduce(R r1, R r2);
  }

  /** The reduce function to use. */
  protected final Reduce<R> reduceFunction;

  /** The result to return if no other result is provided. It should be immutable. */
  protected final R defaultResult;

  /**
   * Constructs an AnnotatedTypeScanner with the given reduce function. If {@code reduceFunction} is
   * null, then the reduce function returns the first result if it is nonnull; otherwise the second
   * result is returned.
   *
   * @param reduceFunction function used to combine two results
   * @param defaultResult the result to return if a visit type method is not overridden; it should
   *     be immutable
   */
  protected AnnotatedTypeScanner(@Nullable Reduce<R> reduceFunction, R defaultResult) {
    if (reduceFunction == null) {
      this.reduceFunction = (r1, r2) -> r1 == null ? r2 : r1;
    } else {
      this.reduceFunction = reduceFunction;
    }
    this.defaultResult = defaultResult;
  }

  /**
   * Constructs an AnnotatedTypeScanner with the given reduce function. If {@code reduceFunction} is
   * null, then the reduce function returns the first result if it is nonnull; otherwise the second
   * result is returned. The default result is {@code null}
   *
   * @param reduceFunction function used to combine two results
   */
  protected AnnotatedTypeScanner(@Nullable Reduce<R> reduceFunction) {
    this(reduceFunction, null);
  }

  /**
   * Constructs an AnnotatedTypeScanner where the reduce function returns the first result if it is
   * nonnull; otherwise the second result is returned.
   *
   * @param defaultResult the result to return if a visit type method is not overridden; it should
   *     be immutable
   */
  protected AnnotatedTypeScanner(R defaultResult) {
    this(null, defaultResult);
  }

  /**
   * Constructs an AnnotatedTypeScanner where the reduce function returns the first result if it is
   * nonnull; otherwise the second result is returned. The default result is {@code null}.
   */
  protected AnnotatedTypeScanner() {
    this(null, null);
  }

  // To prevent infinite loops
  protected final IdentityHashMap<AnnotatedTypeMirror, R> visitedNodes = new IdentityHashMap<>();

  /**
   * Reset the scanner to allow reuse of the same instance. Subclasses should override this method
   * to clear their additional state; they must call the super implementation.
   */
  public void reset() {
    visitedNodes.clear();
  }

  /**
   * Calls {@link #reset()} and then scans {@code type} using null as the parameter.
   *
   * @param type type to scan
   * @return result of scanning {@code type}
   */
  @Override
  public final R visit(AnnotatedTypeMirror type) {
    return visit(type, null);
  }

  /**
   * Calls {@link #reset()} and then scans {@code type} using {@code p} as the parameter.
   *
   * @param type the type to visit
   * @param p a visitor-specified parameter
   * @return result of scanning {@code type}
   */
  @Override
  public final R visit(AnnotatedTypeMirror type, P p) {
    reset();
    return scan(type, p);
  }

  /**
   * Scan {@code type} by calling {@code type.accept(this, p)}; this method may be overridden by
   * subclasses.
   *
   * @param type type to scan
   * @param p the parameter to use
   * @return the result of visiting {@code type}
   */
  protected R scan(AnnotatedTypeMirror type, P p) {
    return type.accept(this, p);
  }

  /**
   * Scan all the types and returns the reduced result.
   *
   * @param types types to scan
   * @param p the parameter to use
   * @return the reduced result of scanning all the types
   */
  protected R scan(@Nullable Iterable<? extends AnnotatedTypeMirror> types, P p) {
    if (types == null) {
      return defaultResult;
    }
    R r = defaultResult;
    boolean first = true;
    for (AnnotatedTypeMirror type : types) {
      r = (first ? scan(type, p) : scanAndReduce(type, p, r));
      first = false;
    }
    return r;
  }

  protected R scanAndReduce(Iterable<? extends AnnotatedTypeMirror> types, P p, R r) {
    return reduce(scan(types, p), r);
  }

  /**
   * Scans {@code type} with the parameter {@code p} and reduces the result with {@code r}.
   *
   * @param type type to scan
   * @param p parameter to use for when scanning {@code type}
   * @param r result to combine with the result of scanning {@code type}
   * @return the combination of {@code r} with the result of scanning {@code type}
   */
  protected R scanAndReduce(AnnotatedTypeMirror type, P p, R r) {
    return reduce(scan(type, p), r);
  }

  /**
   * Combines {@code r1} and {@code r2} and returns the result. The default implementation returns
   * {@code r1} if it is not null; otherwise, it returns {@code r2}.
   *
   * @param r1 a result of scan, nonnull if {@link #defaultResult} is nonnull and this method never
   *     returns null
   * @param r2 a result of scan, nonnull if {@link #defaultResult} is nonnull and this method never
   *     returns null
   * @return the combination of {@code r1} and {@code r2}
   */
  protected R reduce(R r1, R r2) {
    return reduceFunction.reduce(r1, r2);
  }

  @Override
  public R visitDeclared(AnnotatedDeclaredType type, P p) {
    // Only declared types with type arguments might be recursive,
    // so only store those.
    boolean shouldStoreType = !type.getTypeArguments().isEmpty();
    if (shouldStoreType && visitedNodes.containsKey(type)) {
      return visitedNodes.get(type);
    }
    if (shouldStoreType) {
      visitedNodes.put(type, defaultResult);
    }
    R r = defaultResult;
    if (type.getEnclosingType() != null) {
      r = scan(type.getEnclosingType(), p);
      if (shouldStoreType) {
        visitedNodes.put(type, r);
      }
    }
    r = scanAndReduce(type.getTypeArguments(), p, r);
    if (shouldStoreType) {
      visitedNodes.put(type, r);
    }
    return r;
  }

  @Override
  public R visitIntersection(AnnotatedIntersectionType type, P p) {
    if (visitedNodes.containsKey(type)) {
      return visitedNodes.get(type);
    }
    visitedNodes.put(type, defaultResult);
    R r = scan(type.getBounds(), p);
    visitedNodes.put(type, r);
    return r;
  }

  @Override
  public R visitUnion(AnnotatedUnionType type, P p) {
    if (visitedNodes.containsKey(type)) {
      return visitedNodes.get(type);
    }
    visitedNodes.put(type, defaultResult);
    R r = scan(type.getAlternatives(), p);
    visitedNodes.put(type, r);
    return r;
  }

  @Override
  public R visitArray(AnnotatedArrayType type, P p) {
    R r = scan(type.getComponentType(), p);
    return r;
  }

  @Override
  public R visitExecutable(AnnotatedExecutableType type, P p) {
    R r = scan(type.getReturnType(), p);
    if (type.getReceiverType() != null) {
      r = scanAndReduce(type.getReceiverType(), p, r);
    }
    r = scanAndReduce(type.getParameterTypes(), p, r);
    r = scanAndReduce(type.getThrownTypes(), p, r);
    r = scanAndReduce(type.getTypeVariables(), p, r);
    return r;
  }

  @Override
  public R visitTypeVariable(AnnotatedTypeVariable type, P p) {
    if (visitedNodes.containsKey(type)) {
      return visitedNodes.get(type);
    }
    visitedNodes.put(type, defaultResult);
    R r = scan(type.getLowerBound(), p);
    visitedNodes.put(type, r);
    r = scanAndReduce(type.getUpperBound(), p, r);
    visitedNodes.put(type, r);
    return r;
  }

  @Override
  public R visitNoType(AnnotatedNoType type, P p) {
    return defaultResult;
  }

  @Override
  public R visitNull(AnnotatedNullType type, P p) {
    return defaultResult;
  }

  @Override
  public R visitPrimitive(AnnotatedPrimitiveType type, P p) {
    return defaultResult;
  }

  @Override
  public R visitWildcard(AnnotatedWildcardType type, P p) {
    if (visitedNodes.containsKey(type)) {
      return visitedNodes.get(type);
    }
    visitedNodes.put(type, defaultResult);
    R r = scan(type.getExtendsBound(), p);
    visitedNodes.put(type, r);
    r = scanAndReduce(type.getSuperBound(), p, r);
    visitedNodes.put(type, r);
    return r;
  }
}
