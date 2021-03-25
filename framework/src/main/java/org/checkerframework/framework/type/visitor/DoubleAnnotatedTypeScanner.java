package org.checkerframework.framework.type.visitor;

import java.util.Iterator;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.BugInCF;

/**
 * An {@link AnnotatedTypeScanner} that scans two {@link AnnotatedTypeMirror}s simultaneously and
 * performs {@link #defaultAction(AnnotatedTypeMirror, AnnotatedTypeMirror)} on the pair. Both
 * AnnotatedTypeMirrors must have the same structure, or a subclass must arrange not to continue
 * recursing past the point at which their structure diverges.
 *
 * <p>If the default action does not return a result, then {@code R} should be {@link Void} and
 * {@link #DoubleAnnotatedTypeScanner()} should be used to construct the scanner. If the default
 * action returns a result, then specify a {@link #reduce} function and use {@link
 * #DoubleAnnotatedTypeScanner(Reduce, Object)}.
 *
 * @see AnnotatedTypeScanner
 * @param <R> the result of scanning the two {@code AnnotatedTypeMirror}s
 */
public abstract class DoubleAnnotatedTypeScanner<R>
        extends AnnotatedTypeScanner<R, AnnotatedTypeMirror> {

    /**
     * Constructs an AnnotatedTypeScanner where the reduce function returns the first result if it
     * is nonnull; otherwise the second result is returned. The default result is {@code null}.
     */
    protected DoubleAnnotatedTypeScanner() {
        super();
    }

    /**
     * Creates a scanner with the given {@code reduce} function and {@code defaultResult}.
     *
     * @param reduce function used to combine the results of scan
     * @param defaultResult result to use by default
     */
    protected DoubleAnnotatedTypeScanner(Reduce<R> reduce, R defaultResult) {
        super(reduce, defaultResult);
    }

    /**
     * Called by default for any visit method that is not overridden.
     *
     * @param type the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    protected abstract R defaultAction(AnnotatedTypeMirror type, AnnotatedTypeMirror p);

    /**
     * Scans {@code types1} and {@code types2}. If they are empty, then {@link #defaultResult} is
     * returned.
     *
     * @param types1 types
     * @param types2 types
     * @return the result of scanning and reducing all the types in {@code types1} and {@code
     *     types2} or {@link #defaultResult} if they are empty
     */
    protected R scan(
            Iterable<? extends AnnotatedTypeMirror> types1,
            Iterable<? extends AnnotatedTypeMirror> types2) {
        if (types1 == null || types2 == null) {
            return defaultResult;
        }
        R r = defaultResult;
        boolean first = true;
        Iterator<? extends AnnotatedTypeMirror> iter1 = types1.iterator();
        Iterator<? extends AnnotatedTypeMirror> iter2 = types2.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
            r =
                    (first
                            ? scan(iter1.next(), iter2.next())
                            : scanAndReduce(iter1.next(), iter2.next(), r));
            first = false;
        }
        return r;
    }

    /**
     * Run {@link #scan} on types and p, then run {@link #reduce} on the result (plus r) to return a
     * single element.
     */
    protected R scanAndReduce(
            Iterable<? extends AnnotatedTypeMirror> types,
            Iterable<? extends AnnotatedTypeMirror> p,
            R r) {
        return reduce(scan(types, p), r);
    }

    @Override
    protected final R scanAndReduce(
            Iterable<? extends AnnotatedTypeMirror> types, AnnotatedTypeMirror p, R r) {
        throw new BugInCF(
                "DoubleAnnotatedTypeScanner.scanAndReduce: "
                        + p
                        + " is not Iterable<? extends AnnotatedTypeMirror>");
    }

    @Override
    protected R scan(AnnotatedTypeMirror type, AnnotatedTypeMirror p) {
        return reduce(super.scan(type, p), defaultAction(type, p));
    }

    @Override
    public final R visitDeclared(AnnotatedDeclaredType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedDeclaredType : p;
        R r = scan(type.getTypeArguments(), ((AnnotatedDeclaredType) p).getTypeArguments());
        if (type.getEnclosingType() != null) {
            r =
                    scanAndReduce(
                            type.getEnclosingType(),
                            ((AnnotatedDeclaredType) p).getEnclosingType(),
                            r);
        }
        return r;
    }

    @Override
    public final R visitArray(AnnotatedArrayType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedArrayType : p;
        R r = scan(type.getComponentType(), ((AnnotatedArrayType) p).getComponentType());
        return r;
    }

    @Override
    public final R visitExecutable(AnnotatedExecutableType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedExecutableType : p;
        AnnotatedExecutableType ex = (AnnotatedExecutableType) p;
        R r = scan(type.getReturnType(), ex.getReturnType());
        if (type.getReceiverType() != null) {
            r = scanAndReduce(type.getReceiverType(), ex.getReceiverType(), r);
        }
        r = scanAndReduce(type.getParameterTypes(), ex.getParameterTypes(), r);
        r = scanAndReduce(type.getThrownTypes(), ex.getThrownTypes(), r);
        r = scanAndReduce(type.getTypeVariables(), ex.getTypeVariables(), r);
        return r;
    }

    @Override
    public R visitTypeVariable(AnnotatedTypeVariable type, AnnotatedTypeMirror p) {
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }
        visitedNodes.put(type, null);

        R r;
        if (p instanceof AnnotatedTypeVariable) {
            AnnotatedTypeVariable tv = (AnnotatedTypeVariable) p;
            r = scan(type.getLowerBound(), tv.getLowerBound());
            visitedNodes.put(type, r);
            r = scanAndReduce(type.getUpperBound(), tv.getUpperBound(), r);
            visitedNodes.put(type, r);
        } else {
            r = scan(type.getLowerBound(), p.getErased());
            visitedNodes.put(type, r);
            r = scanAndReduce(type.getUpperBound(), p.getErased(), r);
            visitedNodes.put(type, r);
        }
        return r;
    }

    @Override
    public R visitWildcard(AnnotatedWildcardType type, AnnotatedTypeMirror p) {
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }
        visitedNodes.put(type, null);

        R r;
        if (p instanceof AnnotatedWildcardType) {
            AnnotatedWildcardType w = (AnnotatedWildcardType) p;
            r = scan(type.getExtendsBound(), w.getExtendsBound());
            visitedNodes.put(type, r);
            r = scanAndReduce(type.getSuperBound(), w.getSuperBound(), r);
            visitedNodes.put(type, r);
        } else {
            r = scan(type.getExtendsBound(), p.getErased());
            visitedNodes.put(type, r);
            r = scanAndReduce(type.getSuperBound(), p.getErased(), r);
            visitedNodes.put(type, r);
        }
        return r;
    }

    @Override
    public R visitIntersection(AnnotatedIntersectionType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedIntersectionType : p;

        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }
        visitedNodes.put(type, null);
        R r = scan(type.getBounds(), ((AnnotatedIntersectionType) p).getBounds());
        return r;
    }

    @Override
    public R visitUnion(AnnotatedUnionType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedUnionType : p;
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }
        visitedNodes.put(type, null);
        R r = scan(type.getAlternatives(), ((AnnotatedUnionType) p).getAlternatives());
        return r;
    }
}
