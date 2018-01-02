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
import org.checkerframework.javacutil.ErrorReporter;

/**
 * A TypeVisitor that takes two AnnotatedTypeMirrors as parameters, and visits them simultaneously.
 * Both Annotated Type Mirrors must have the same structure or else the various asserts will fail.
 *
 * @see AnnotatedTypeScanner
 */
public abstract class AnnotatedTypeComparer<R>
        extends AnnotatedTypeScanner<R, AnnotatedTypeMirror> {
    /** Compares two annotated type mirrors. */
    protected abstract R compare(AnnotatedTypeMirror type, AnnotatedTypeMirror p);

    /** Supplies the logic to reduce on how to combine two R objects */
    protected abstract R combineRs(R r1, R r2);

    protected R scan(
            Iterable<? extends AnnotatedTypeMirror> types,
            Iterable<? extends AnnotatedTypeMirror> p) {
        if (types == null) {
            return null;
        }
        R r = null;
        boolean first = true;
        Iterator<? extends AnnotatedTypeMirror> tIter = types.iterator(), pIter = p.iterator();
        while (tIter.hasNext() && pIter.hasNext()) {
            r =
                    (first
                            ? scan(tIter.next(), pIter.next())
                            : scanAndReduce(tIter.next(), pIter.next(), r));
            first = false;
        }
        return r;
    }

    @Override
    protected R reduce(R r1, R r2) {
        return combineRs(r1, r2);
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
    protected R scanAndReduce(
            Iterable<? extends AnnotatedTypeMirror> types, AnnotatedTypeMirror p, R r) {
        ErrorReporter.errorAbort(
                "AnnotatedTypeComparer.scanAndReduce: "
                        + p
                        + "is not Iterable<? extends AnnotatedTypeMirror>");
        return null;
    }

    @Override
    protected R scan(AnnotatedTypeMirror type, AnnotatedTypeMirror p) {
        return reduce(super.scan(type, p), compare(type, p));
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
        R r;
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }

        visitedNodes.put(type, null);

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
        assert p instanceof AnnotatedWildcardType : p;
        AnnotatedWildcardType w = (AnnotatedWildcardType) p;
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }

        visitedNodes.put(type, null);
        R r = scan(type.getExtendsBound(), w.getExtendsBound());
        visitedNodes.put(type, r);
        r = scanAndReduce(type.getSuperBound(), w.getSuperBound(), r);
        visitedNodes.put(type, r);
        return r;
    }

    @Override
    public R visitIntersection(AnnotatedIntersectionType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedIntersectionType : p;

        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }
        visitedNodes.put(type, null);
        R r = scan(type.directSuperTypes(), ((AnnotatedIntersectionType) p).directSuperTypes());
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
