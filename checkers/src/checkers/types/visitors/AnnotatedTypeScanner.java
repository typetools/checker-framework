package checkers.types.visitors;

import java.util.IdentityHashMap;
import java.util.Map;

import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.*;


/**
 * A TypeVisitor that visits all the child tree nodes. To visit types
 * of a particular type, just override the corresponding visitXYZ
 * method. Inside your method, call super.visitXYZ to visit descendant
 * nodes.
 *
 * The default implementation of the visitXYZ methods will determine a
 * result as follows:
 *
 * If the node being visited has no children, the result will be null.
 * If the node being visited has one child, the result will be the
 * result of calling scan on that child. The child may be a simple
 * node or itself a list of nodes. If the node being visited has more
 * than one child, the result will be determined by calling scan each
 * child in turn, and then combining the result of each scan after the
 * first with the cumulative result so far, as determined by the
 * reduce(R, R) method. Each child may be either a simple node of a
 * list of nodes. The default behavior of the reduce method is such
 * that the result of the visitXYZ method will be the result of the
 * last child scanned.
 *
 * Here is an example to count the parameter types number of nodes in
 * a tree:
 *
 * <pre>
 * class CountTypeVariable extends TreeScanner {
 *
 *     &#64;Override
 *     public Integer visitTypeVariable(ATypeVariable node, Void p) {
 *         return 1;
 *     }
 *
 *     &#64;Override
 *     public Integer reduce(Integer r1, Integer r2) {
 *         return (r1 == null ? 0 : r1) + (r2 == null ? 0 : r2);
 *     }
 * }
 * </pre>
 *
 * @param <R>
 *            the return type of this visitor's methods. Use Void for
 *            visitors that do not need to return results.
 * @param <P>
 *            the type of the additional parameter to this visitor's
 *            methods. Use Void for visitors that do not need an
 *            additional parameter.
 */
public class AnnotatedTypeScanner<R, P> implements AnnotatedTypeVisitor<R, P> {

    // To prevent infinite loops
    protected Map<AnnotatedTypeMirror, R> visitedNodes =
        new IdentityHashMap<AnnotatedTypeMirror, R>();

    @Override
    public final R visit(AnnotatedTypeMirror t) {
        return visit(t, null);
    }

    @Override
    public final R visit(AnnotatedTypeMirror type, P p) {
        Map<AnnotatedTypeMirror, R> prev = visitedNodes;
        visitedNodes = new IdentityHashMap<AnnotatedTypeMirror, R>();
        try {
            return scan(type, p);
        } finally {
            visitedNodes = prev;
        }
    }

    /**
     * Processes an element by calling e.accept(this, p); this method
     * may be overridden by subclasses.
     *
     * @return the result of visiting {@code type}
     */
    protected R scan(AnnotatedTypeMirror type, P p) {
        return (type == null ? null : type.accept(this, p));
    }

    /**
     * Processes an element by calling e.accept(this, p); this method
     * may be overridden by subclasses.
     *
     * @param types
     * @param p
     * @return a visitor-specified result
     */
    protected R scan(Iterable<? extends AnnotatedTypeMirror> types, P p) {
        if (types == null)
            return null;
        R r = null;
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

    public R scanAndReduce(AnnotatedTypeMirror type, P p, R r) {
        return reduce(scan(type, p), r);
    }

    protected R reduce(R r1, R r2) {
        if (r1 == null)
            return r2;
        return r1;
    }

    @Override
    public R visitDeclared(AnnotatedDeclaredType type, P p) {
        R r = scan(type.getTypeArguments(), p);
        /*
         * TODO: Would it be good to do the following here? I only do this in
         * QualifierDefaults now, but maybe more general would be better?
         * But the IGJ Checker loops (again...) if I add this.
        if (TypesUtils.isAnonymousType(type.getUnderlyingType())) {
            for(AnnotatedDeclaredType adt : type.directSuperTypes()) {
                r = scanAndReduce(adt, p, r);
            }
        }*/
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
        r = scanAndReduce(type.getReceiverType(), p, r);
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
        R r = scan(type.getLowerBoundField(), p);
        visitedNodes.put(type, r);
        r = scanAndReduce(type.getUpperBoundField(), p, r);
        visitedNodes.put(type, r);
        return r;
    }

    @Override
    public R visitNoType(AnnotatedNoType type, P p) {
        return null;
    }

    @Override
    public R visitNull(AnnotatedNullType type, P p) {
        return null;
    }

    @Override
    public R visitPrimitive(AnnotatedPrimitiveType type, P p) {
        return null;
    }

    @Override
    public R visitWildcard(AnnotatedWildcardType type, P p) {
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }
        R r = scan(type.getExtendsBoundField(), p);
        visitedNodes.put(type, r);
        r = scanAndReduce(type.getSuperBoundField(), p, r);
        visitedNodes.put(type, r);
        return r;
    }
}
