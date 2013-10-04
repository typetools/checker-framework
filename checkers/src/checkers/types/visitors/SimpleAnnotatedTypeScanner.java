package checkers.types.visitors;

import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.*;

public class SimpleAnnotatedTypeScanner<R, P> extends AnnotatedTypeScanner<R, P> {

    /**
     * Called by default for any visit method that is not overridden.
     *
     * @param type the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    protected R defaultAction(AnnotatedTypeMirror type, P p) {
        // Do nothing
        return null;
    }

    /**
     * Visits a declared type.
     *
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    @Override
    public final R visitDeclared(AnnotatedDeclaredType type, P p) {
        defaultAction(type, p);
        return super.visitDeclared(type, p);
    }

    /**
     * Visits an executable type.
     *
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    @Override
    public final R visitExecutable(AnnotatedExecutableType type, P p) {
        defaultAction(type, p);
        return super.visitExecutable(type, p);
    }

    /**
     * Visits an array type.
     *
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    @Override
    public final R visitArray(AnnotatedArrayType type, P p) {
        defaultAction(type, p);
        return super.visitArray(type, p);
    }

    /**
     * Visits a type variable.
     *
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    @Override
    public final R visitTypeVariable(AnnotatedTypeVariable type, P p) {
        defaultAction(type, p);
        return super.visitTypeVariable(type, p);
    }

    /**
     * Visits a primitive type.
     *
     * @param type  the type to visit
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
     * @param type  the type to visit
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
     * @param type  the type to visit
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
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    @Override
    public final R visitWildcard(AnnotatedWildcardType type, P p) {
        defaultAction(type, p);
        return super.visitWildcard(type, p);
    }

}
