package checkers.types.visitors;

import java.util.*;
import java.lang.UnsupportedOperationException;

import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.*;

/**
 * A TypeVisitor that takes an AnnotatedTypeMirror as a parameter, and
 * visits it simultaneously.  Note that the parameter type must be
 * assignable to the visited type, or an assertion fails when visiting
 * an incompatible subtype.
 *
 * @see AnnotatedTypeScanner
 */
public class AnnotatedTypeComparer<R> extends AnnotatedTypeScanner<R, AnnotatedTypeMirror> {

    protected R scan(Iterable<? extends AnnotatedTypeMirror> types,
                     Iterable<? extends AnnotatedTypeMirror> p) {
        if (types == null)
            return null;
        R r = null;
        boolean first = true;
        Iterator<? extends AnnotatedTypeMirror> tIter = types.iterator(),
            pIter = p.iterator();
        while (tIter.hasNext() && pIter.hasNext()) {
            r = (first ? scan(tIter.next(), pIter.next())
                 : scanAndReduce(tIter.next(), pIter.next(), r));
            first = false;
        }
        return r;
    }

    protected R scanAndReduce(Iterable<? extends AnnotatedTypeMirror> types, Iterable<? extends AnnotatedTypeMirror> p, R r) {
        return reduce(scan(types, p), r);
    }

    @Override
    public R scanAndReduce(AnnotatedTypeMirror type, AnnotatedTypeMirror p, R r) {
        return reduce(scan(type, p), r);
    }

    @Override
    protected R scanAndReduce(Iterable<? extends AnnotatedTypeMirror> types, AnnotatedTypeMirror p, R r) {
        SourceChecker.errorAbort("AnnotatedTypeComparer.scanAndReduce: " + p + "is not Iterable<? extends AnnotatedTypeMirror>");
        return null;
    }


    @Override
    public final R visitDeclared(AnnotatedDeclaredType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedDeclaredType : p;
        R r = scan(type.getTypeArguments(),
                   ((AnnotatedDeclaredType)p).getTypeArguments());
        return r;
    }

    @Override
    public final R visitArray(AnnotatedArrayType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedArrayType : p;
        R r = scan(type.getComponentType(),
                   ((AnnotatedArrayType)p).getComponentType());
        return r;
    }

    @Override
    public final R visitExecutable(AnnotatedExecutableType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedExecutableType : p;
        AnnotatedExecutableType ex = (AnnotatedExecutableType) p;
        R r = scan(type.getReturnType(), ex.getReturnType());
        r = scanAndReduce(type.getReceiverType(), ex.getReceiverType(), r);
        r = scanAndReduce(type.getParameterTypes(), ex.getParameterTypes(), r);
        r = scanAndReduce(type.getThrownTypes(), ex.getThrownTypes(), r);
        r = scanAndReduce(type.getTypeVariables(), ex.getTypeVariables(), r);
        return r;
    }

    @Override
    public R visitTypeVariable(AnnotatedTypeVariable type, AnnotatedTypeMirror p) {
        // assert p instanceof AnnotatedTypeVariable : p;
        R r;
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }
        if (p instanceof AnnotatedTypeVariable) {
            AnnotatedTypeVariable tv = (AnnotatedTypeVariable) p;
            r = scan(type.getLowerBound(), tv.getLowerBound());
            visitedNodes.put(type, r);
            r = scanAndReduce(type.getUpperBound(), tv.getUpperBound(), r);
            visitedNodes.put(type, r);
        } else {
            r = scan(type.getLowerBound(), p.getErased());
            visitedNodes.put(type, r);
            r = scanAndReduce(type.getUpperBound(), p, r);
            visitedNodes.put(type, r);
        }
        return r;
    }

    @Override
    public final R visitWildcard(AnnotatedWildcardType type, AnnotatedTypeMirror p) {
        assert p instanceof AnnotatedWildcardType : p;
        AnnotatedWildcardType w = (AnnotatedWildcardType) p;
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }
        R r = scan(type.getExtendsBound(), w.getExtendsBound());
        visitedNodes.put(type, r);
        r = scanAndReduce(type.getSuperBound(), w.getSuperBound(), r);
        visitedNodes.put(type, r);
        return r;
    }
}
