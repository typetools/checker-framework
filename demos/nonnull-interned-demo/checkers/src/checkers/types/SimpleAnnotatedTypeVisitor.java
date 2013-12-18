package checkers.types;

import checkers.types.AnnotatedTypeMirror.*;


public class SimpleAnnotatedTypeVisitor<R, P> implements AnnotatedTypeVisitor<R, P> {

    protected R DEFAULT_VALUE;
    
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
