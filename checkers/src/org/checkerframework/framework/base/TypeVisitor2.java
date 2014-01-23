package org.checkerframework.framework.base;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public abstract class TypeVisitor2<R,P> {
    public R visit(TypeMirror type) {
        return visit(type, null);
    }

    public R visit(TypeMirror type, P p) {
        switch (type.getKind()) {
            case ARRAY:
                return visitArray((ArrayType)type, p);
            case DECLARED:
                return visitDeclared((DeclaredType)type, p);
            case ERROR:
                return visitError((ErrorType)type, p);
            case EXECUTABLE:
                return visitExecutable((ExecutableType)type, p);
            case INTERSECTION:
                return visitIntersection((IntersectionType)type, p);
            case NONE:
                return visitNoType((NoType)type, p);
            case NULL:
                return visitNull((NullType)type, p);
            case PACKAGE:
                return visitNoType((NoType)type, p);
            case TYPEVAR:
                return visitTypeVariable((TypeVariable)type, p);
            case UNION:
                return visitUnion((UnionType)type, p);
            case VOID:
                return visitNoType((NoType)type, p);
            case WILDCARD:
                return visitWildcard((WildcardType)type, p);
            default:
                if (type.getKind().isPrimitive()) {
                    return visitPrimitive((PrimitiveType)type, p);
                } else {
                    return visitUnknown(type, p);
                }
        }
    }

    public abstract R visitArray(ArrayType type, P p);
    public abstract R visitDeclared(DeclaredType type, P p);
    public abstract R visitError(ErrorType type, P p);
    public abstract R visitExecutable(ExecutableType type, P p);
    public abstract R visitIntersection(IntersectionType type, P p);
    public abstract R visitNoType(NoType type, P p);
    public abstract R visitNull(NullType type, P p);
    public abstract R visitPrimitive(PrimitiveType type, P p);
    public abstract R visitTypeVariable(TypeVariable type, P p);
    public abstract R visitUnion(UnionType type, P p);
    public abstract R visitWildcard(WildcardType type, P p);

    public R visitUnknown(TypeMirror type, P p) {
        throw new UnsupportedOperationException(
                "can't handle TypeMirrors of unknown kind " + type.getKind());
    }
}
