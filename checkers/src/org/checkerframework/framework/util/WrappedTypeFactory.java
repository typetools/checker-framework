package org.checkerframework.framework.util;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
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
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.checkerframework.framework.base.TypeVisitor2;
import org.checkerframework.framework.util.WrappedTypeMirror;
import static org.checkerframework.framework.util.WrappedTypeMirror.*;

public class WrappedTypeFactory {
    private WrappedDeclaredType wrappedObject;
    private WrappedNullType wrappedNull;

    public WrappedTypeFactory(Elements elements, Types types) {
        DeclaredType rawObject = types.getDeclaredType(elements.getTypeElement("java.lang.Object"));
        this.wrappedObject = this.wrap(rawObject);

        NullType rawNull = types.getNullType();
        this.wrappedNull = this.wrap(rawNull);
    }


    public WrappedDeclaredType getWrappedObject() {
        return this.wrappedObject;
    }

    public WrappedNullType getWrappedNull() {
        return this.wrappedNull;
    }


    public WrappedTypeMirror wrap(TypeMirror type) {
        return this.wrap(type, null);
    }

    public WrappedTypeMirror wrap(TypeMirror type, Element elt) {
        if (type == null) {
            return null;
        }
        return VISITOR.visit(type, elt);
    }

    public WrappedArrayType wrap(ArrayType type) {
        return (WrappedArrayType)this.wrap((TypeMirror)type);
    }

    public WrappedDeclaredType wrap(DeclaredType type) {
        return (WrappedDeclaredType)this.wrap((TypeMirror)type);
    }

    public WrappedExecutableType wrap(ExecutableType type) {
        return (WrappedExecutableType)this.wrap((TypeMirror)type);
    }

    public WrappedExecutableType wrap(ExecutableType type, Element elt) {
        return (WrappedExecutableType)this.wrap((TypeMirror)type, elt);
    }

    public WrappedIntersectionType wrap(IntersectionType type) {
        return (WrappedIntersectionType)this.wrap((TypeMirror)type);
    }

    public WrappedNoType wrap(NoType type) {
        return (WrappedNoType)this.wrap((TypeMirror)type);
    }

    public WrappedNullType wrap(NullType type) {
        return (WrappedNullType)this.wrap((TypeMirror)type);
    }

    public WrappedPrimitiveType wrap(PrimitiveType type) {
        return (WrappedPrimitiveType)this.wrap((TypeMirror)type);
    }

    public WrappedTypeVariable wrap(TypeVariable type) {
        return (WrappedTypeVariable)this.wrap((TypeMirror)type);
    }

    public WrappedUnionType wrap(UnionType type) {
        return (WrappedUnionType)this.wrap((TypeMirror)type);
    }

    public WrappedWildcardType wrap(WildcardType type) {
        return (WrappedWildcardType)this.wrap((TypeMirror)type);
    }


    public List<WrappedTypeMirror> wrap(List<? extends TypeMirror> types) {
        if (types == null) {
            return null;
        }

        ArrayList<WrappedTypeMirror> result = new ArrayList<>(types.size());
        for (TypeMirror type : types) {
            result.add(this.wrap(type));
        }
        return result;
    }

    public List<WrappedTypeVariable> wrapTypeVariables(List<? extends TypeVariable> types) {
        if (types == null) {
            return null;
        }

        ArrayList<WrappedTypeVariable> result = new ArrayList<>(types.size());
        for (TypeVariable type : types) {
            result.add(this.wrap(type));
        }
        return result;
    }


    private TypeVisitor2<WrappedTypeMirror, Element> VISITOR =
        new TypeVisitor2<WrappedTypeMirror, Element>() {
            @Override
            public WrappedTypeMirror visitArray(ArrayType type, Element elt) {
                return new WrappedArrayType(type, WrappedTypeFactory.this);
            }

            @Override
            public WrappedTypeMirror visitDeclared(DeclaredType type, Element elt) {
                return new WrappedDeclaredType(type, WrappedTypeFactory.this);
            }

            @Override
            public WrappedTypeMirror visitError(ErrorType type, Element elt) {
                return new WrappedErrorType(type, WrappedTypeFactory.this);
            }

            @Override
            public WrappedTypeMirror visitExecutable(ExecutableType type, Element elt) {
                return new WrappedExecutableType(type, elt, WrappedTypeFactory.this);
            }

            @Override
            public WrappedTypeMirror visitIntersection(IntersectionType type, Element elt) {
                return new WrappedIntersectionType(type, WrappedTypeFactory.this);
            }

            @Override
            public WrappedTypeMirror visitNoType(NoType type, Element elt) {
                return new WrappedNoType(type, WrappedTypeFactory.this);
            }

            @Override
            public WrappedTypeMirror visitNull(NullType type, Element elt) {
                return new WrappedNullType(type, WrappedTypeFactory.this);
            }

            @Override
            public WrappedTypeMirror visitPrimitive(PrimitiveType type, Element elt) {
                return new WrappedPrimitiveType(type, WrappedTypeFactory.this);
            }

            @Override
            public WrappedTypeMirror visitTypeVariable(TypeVariable type, Element elt) {
                return new WrappedTypeVariable(type, WrappedTypeFactory.this);
            }

            @Override
            public WrappedTypeMirror visitUnion(UnionType type, Element elt) {
                return new WrappedUnionType(type, WrappedTypeFactory.this);
            }

            @Override
            public WrappedTypeMirror visitWildcard(WildcardType type, Element elt) {
                return new WrappedWildcardType(type, WrappedTypeFactory.this);
            }
        };

}
