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

/**
 * A simple implementation of AnnotatedTypeScanner. It is abstract; its purpose is to provide
 * default implementations of all methods.
 */
public class SimpleAnnotatedTypeScanner<R, P> extends AnnotatedTypeScanner<R, P> {

    @FunctionalInterface
    public interface DefaultAction<R, P> {
        R defaultAction(AnnotatedTypeMirror type, P p);
    }

    protected final DefaultAction<R, P> defaultAction;

    public SimpleAnnotatedTypeScanner(DefaultAction<R, P> defaultAction, Reduce<R> reduce) {
        super(reduce);
        if (defaultAction == null) {
            this.defaultAction = (t, p) -> defaultResult;
        } else {
            this.defaultAction = defaultAction;
        }
    }

    public SimpleAnnotatedTypeScanner() {
        this(null, null);
    }

    public SimpleAnnotatedTypeScanner(Reduce<R> reduce) {
        this(null, reduce);
    }

    public SimpleAnnotatedTypeScanner(DefaultAction<R, P> defaultAction) {
        this(defaultAction, null);
    }

    /**
     * Called by default for any visit method that is not overridden.
     *
     * @param type the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    protected R defaultAction(AnnotatedTypeMirror type, P p) {
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
