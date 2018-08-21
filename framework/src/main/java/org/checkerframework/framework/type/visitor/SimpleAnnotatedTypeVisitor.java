package org.checkerframework.framework.type.visitor;

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
 * A simple visitor for {@link AnnotatedTypeMirror}s.
 *
 * @param <R> the return type of this visitor's methods. Use {@link Void} for visitors that do not
 *     need to return results.
 * @param <P> the type of the additional parameter to this visitor's methods. Use {@link Void} for
 *     visitors that do not need an additional parameter.
 */
public class SimpleAnnotatedTypeVisitor<R, P> implements AnnotatedTypeVisitor<R, P> {

    /** The default value to return as a default action. */
    protected final R DEFAULT_VALUE;

    /**
     * Creates an instance of {@link SimpleAnnotatedTypeVisitor} with default value being {@code
     * null}.
     */
    public SimpleAnnotatedTypeVisitor() {
        this(null);
    }

    /**
     * Creates an instance of {@link SimpleAnnotatedTypeVisitor} with the default value being the
     * passed defaultValue.
     *
     * @param defaultValue the default value this class should return
     */
    public SimpleAnnotatedTypeVisitor(R defaultValue) {
        this.DEFAULT_VALUE = defaultValue;
    }

    /**
     * Performs the default action for visiting trees, if subclasses do not override the visitFOO
     * node.
     *
     * <p>This implementation merely returns the default value (as specified by the protected field
     * {@code DEFAULT_VALUE}).
     */
    protected R defaultAction(AnnotatedTypeMirror type, P p) {
        return DEFAULT_VALUE;
    }

    @Override
    public R visit(AnnotatedTypeMirror type) {
        return visit(type, null);
    }

    @Override
    public R visit(AnnotatedTypeMirror type, P p) {
        return (type == null) ? null : type.accept(this, p);
    }

    @Override
    public R visitDeclared(AnnotatedDeclaredType type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitIntersection(AnnotatedIntersectionType type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitUnion(AnnotatedUnionType type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitArray(AnnotatedArrayType type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitExecutable(AnnotatedExecutableType type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitTypeVariable(AnnotatedTypeVariable type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitWildcard(AnnotatedWildcardType type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitPrimitive(AnnotatedPrimitiveType type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitNull(AnnotatedNullType type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitNoType(AnnotatedNoType type, P p) {
        return defaultAction(type, p);
    }
}
