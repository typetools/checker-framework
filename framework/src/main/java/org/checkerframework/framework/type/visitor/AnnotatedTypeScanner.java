package org.checkerframework.framework.type.visitor;

import java.util.IdentityHashMap;
import java.util.Map;
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
 * An {@link AnnotatedTypeVisitor} that visits all the child types of the type. Subclasses of this
 * class are used to implement some logical unit of work that varies depending on the kind of
 * AnnotatedTypeMirror.
 *
 * <p>If the unit of work does not return a result, then {@code R} should be instantiated to {@link
 * Void} Override the desired visitXXX methods and usually {@code return super.vistXXX} is the last
 * statement to ensure that composite types are visited. If you do not want composite types to be
 * visited, the just return {@code null}.
 *
 * <p>If the unit of work return a result, then Override {@link #reduce(R, R)} Override the desired
 * visitXXX methods and usually {@code reduce(super.visitArray(type, parameter), result)} is the
 * last statement to ensure that composite types are visited and the result are reduced correctly.
 *
 * <p>The default implementation of the visitXYZ methods will determine a result as follows:
 *
 * <ul>
 *   <li>If the node being visited has no children, the result will be null.
 *   <li>If the node being visited has one child, the result will be the result of calling scan on
 *       that child. The child may be a simple node or itself a list of nodes.
 *   <li>If the node being visited has more than one child, the result will be determined by calling
 *       scan each child in turn, and then combining the result of each scan after the first with
 *       the cumulative result so far, as determined by the reduce(R, R) method. Each child may be
 *       either a simple node or a list of nodes. The default behavior of the reduce method is such
 *       that the result of the visitXYZ method will be the result of the last child scanned.
 * </ul>
 *
 * Here is an example to count the number of TypeVariables in an AnnotatedTypeMirror
 *
 * <pre><code>
 * class CountTypeVariable extends AnnotatedTypeScanner<Integer, Void> {
 *
 *    {@literal @}Override
 *     public Integer visitTypeVariable(AnnotatedTypeVariable type, Void p) {
 *         return reduce(super.visitTypeVariable(type, p), 1);
 *     }
 *
 *    {@literal @}Override
 *     public Integer reduce(Integer r1, Integer r2) {
 *         // The default implementation of visit methods that do not have composite
 *         // types return null, so reduce must expect null.
 *         return (r1 == null ? 0 : r1) + (r2 == null ? 0 : r2);
 *     }
 *
 *     public static int count(AnnotatedTypeMirror type) {
 *         Integer count = new CountTypeVariable().visit(type);
 *         return count == null ? 0 : count;
 *     }
 * }
 * </code></pre>
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
    protected Reduce<R> reduceFunction;

    protected R defaultResult;

    /**
     * Constructs an AnnotatedTypeScanner with the given reduce function. If {@code reduceFunction}
     * is null, then the reduce function returns the first result if it is nonnull; otherwise the
     * second result is returned.
     *
     * @param reduceFunction function used to combine two results
     * @param defaultResult the result to return if a visit type method is not overriden
     */
    public AnnotatedTypeScanner(@Nullable Reduce<R> reduceFunction, R defaultResult) {
        if (reduceFunction == null) {
            this.reduceFunction = (r1, r2) -> r1 == null ? r2 : r1;
        } else {
            this.reduceFunction = reduceFunction;
        }
        this.defaultResult = defaultResult;
    }

    /**
     * Constructs an AnnotatedTypeScanner with the given reduce function. If {@code reduceFunction}
     * is null, then the reduce function returns the first result if it is nonnull; otherwise the
     * second result is returned. The default result is {@code null}
     *
     * @param reduceFunction function used to combine two results
     */
    public AnnotatedTypeScanner(@Nullable Reduce<R> reduceFunction) {
        this(reduceFunction, null);
    }

    /**
     * Constructs an AnnotatedTypeScanner where the reduce function returns the first result if it
     * is nonnull; otherwise the second result is returned.
     *
     * @param defaultResult the result to return if a visit type method is not overriden
     */
    public AnnotatedTypeScanner(R defaultResult) {
        this(null, defaultResult);
    }

    /**
     * Constructs an AnnotatedTypeScanner where the reduce function returns the first result if it
     * is nonnull; otherwise the second result is returned. The default result is {@code null}.
     */
    public AnnotatedTypeScanner() {
        this(null, null);
    }

    // To prevent infinite loops
    protected final Map<AnnotatedTypeMirror, R> visitedNodes = new IdentityHashMap<>();

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
    protected R scan(Iterable<? extends AnnotatedTypeMirror> types, P p) {
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
     * @param r1 a result of scan
     * @param r2 a result of scan
     * @return the combination of {@code r1} and {@code r2}
     */
    protected R reduce(R r1, R r2) {
        return reduceFunction.reduce(r1, r2);
    }

    @Override
    public R visitDeclared(AnnotatedDeclaredType type, P p) {
        boolean shouldStoreType = !type.getTypeArguments().isEmpty();
        // Only declared types with type arguments might be recursive.
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
        R r = scan(type.directSuperTypes(), p);
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
