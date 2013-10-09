package checkers.types;

import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeMirror.AnnotatedNoType;
import checkers.types.AnnotatedTypeMirror.AnnotatedNullType;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.types.AnnotatedTypeMirror.AnnotatedWildcardType;

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
        return defaultAction(type, p);
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
        return defaultAction(type, p);
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
        return defaultAction(type, p);
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
        return defaultAction(type, p);
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
        return defaultAction(type, p);
    }

}
