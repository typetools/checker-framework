package org.checkerframework.framework.base;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.ArrayList;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeMirror.AnnotatedIntersectionType;
import checkers.types.AnnotatedTypeMirror.AnnotatedNoType;
import checkers.types.AnnotatedTypeMirror.AnnotatedNullType;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import checkers.types.AnnotatedTypeMirror.AnnotatedReferenceType;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.types.AnnotatedTypeMirror.AnnotatedUnionType;
import checkers.types.AnnotatedTypeMirror.AnnotatedWildcardType;

import org.checkerframework.framework.util.ExtendedTypeMirror;
import org.checkerframework.framework.util.ExtendedArrayType;
import org.checkerframework.framework.util.ExtendedDeclaredType;
import org.checkerframework.framework.util.ExtendedErrorType;
import org.checkerframework.framework.util.ExtendedExecutableType;
import org.checkerframework.framework.util.ExtendedIntersectionType;
import org.checkerframework.framework.util.ExtendedNoType;
import org.checkerframework.framework.util.ExtendedNullType;
import org.checkerframework.framework.util.ExtendedPrimitiveType;
import org.checkerframework.framework.util.ExtendedReferenceType;
import org.checkerframework.framework.util.ExtendedTypeVariable;
import org.checkerframework.framework.util.ExtendedUnionType;
import org.checkerframework.framework.util.ExtendedWildcardType;
import org.checkerframework.framework.util.ExtendedTypeVisitor;

public abstract class ZippedTypeMirror implements ExtendedTypeMirror {
    private ExtendedTypeMirror base;
    private AnnotatedTypeMirror annotated;

    private ZippedTypeMirror(ExtendedTypeMirror base, AnnotatedTypeMirror annotated) {
        if (base == null) {
            throw new IllegalArgumentException("base TypeMirror must not be null");
        }
        if (annotated == null) {
            throw new IllegalArgumentException("annotated TypeMirror must not be null");
        }
        this.base = base;
        this.annotated = annotated;
    }

    public ExtendedTypeMirror getBase() {
        return base;
    }

    public AnnotatedTypeMirror getAnnotated() {
        return annotated;
    }

    @Override
    public TypeMirror getRaw() {
        return base.getRaw();
    }

    @Override
    public TypeKind getKind() {
        return base.getKind();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return base.getAnnotation(annotationType);
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return base.getAnnotationMirrors();
    }

    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        return base.getAnnotationsByType(annotationType);
    }

    @Override
    public String toString() {
        return base.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        ZippedTypeMirror other = (ZippedTypeMirror)obj;
        return this.base.equals(other.base)
            && this.annotated.equals(other.annotated);
    }

    @Override
    public int hashCode() {
        return base.hashCode() * 17 + annotated.hashCode() * 37;
    }

    public static ExtendedTypeMirror zip(ExtendedTypeMirror base, AnnotatedTypeMirror annotated) {
        if (base == null) {
            return null;
        }

        if (annotated == null || annotated.getKind() != base.getKind()) {
            return base;
        }

        switch (base.getKind()) {
            case ARRAY:
                return new ZippedArrayType((ExtendedArrayType)base, (AnnotatedArrayType)annotated);
            case DECLARED:
                return new ZippedDeclaredType((ExtendedDeclaredType)base, (AnnotatedDeclaredType)annotated);
            case EXECUTABLE:
                return new ZippedExecutableType((ExtendedExecutableType)base, (AnnotatedExecutableType)annotated);
            case VOID:
            case PACKAGE:
            case NONE:
                return new ZippedNoType((ExtendedNoType)base, (AnnotatedNoType)annotated);
            case NULL:
                return new ZippedNullType((ExtendedNullType)base, (AnnotatedNullType)annotated);
            case TYPEVAR:
                return new ZippedTypeVariable((ExtendedTypeVariable)base, (AnnotatedTypeVariable)annotated);
            case WILDCARD:
                return new ZippedWildcardType((ExtendedWildcardType)base, (AnnotatedWildcardType)annotated);
            case INTERSECTION:
                return new ZippedIntersectionType((ExtendedIntersectionType)base, (AnnotatedIntersectionType)annotated);
            case UNION:
                return new ZippedUnionType((ExtendedUnionType)base, (AnnotatedUnionType)annotated);
            default:
                if (base.getKind().isPrimitive()) {
                    return new ZippedPrimitiveType((ExtendedPrimitiveType)base, (AnnotatedPrimitiveType)annotated);
                }
                throw new IllegalArgumentException("unexpected type kind: " + base.getKind());
        }
    }

    private static <ETM extends ExtendedTypeMirror> List<ETM> zipLists(
            List<? extends ETM> bases, List<? extends AnnotatedTypeMirror> annotateds) {
        if (bases == null) {
            return null;
        }

        if (annotateds == null || bases.size() != annotateds.size()) {
            List<ETM> result = new ArrayList<>();
            for (ETM base : bases) {
                result.add(base);
            }
            return result;
        }

        List<ETM> result = new ArrayList<>();
        for (int i = 0; i < bases.size(); ++i) {
            @SuppressWarnings("unchecked")
            ETM resultItem = (ETM)zip(bases.get(i), annotateds.get(i));
            result.add(resultItem);
        }

        return result;
    }

    public static ExtendedTypeMirror unzip(ExtendedTypeMirror type) {
        while (type instanceof ZippedTypeMirror) {
            type = ((ZippedTypeMirror)type).getBase();
        }
        return type;
    }

    public static class ZippedArrayType extends ZippedReferenceType implements ExtendedArrayType {
        private ExtendedTypeMirror componentType;

        public ZippedArrayType(ExtendedArrayType base, AnnotatedArrayType annotated) {
            super(base, annotated);
        }

        @Override
        public ExtendedArrayType getBase() {
            return (ExtendedArrayType)super.getBase();
        }

        @Override
        public AnnotatedArrayType getAnnotated() {
            return (AnnotatedArrayType)super.getAnnotated();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitArray(this, p);
        }

        @Override
        public ExtendedTypeMirror getComponentType() {
            if (componentType == null) {
                componentType = zip(getBase().getComponentType(),
                        getAnnotated().getComponentType());
            }
            return componentType;
        }
    }

    public static class ZippedDeclaredType extends ZippedReferenceType implements ExtendedDeclaredType {
        private ExtendedTypeMirror enclosingType;
        private List<ExtendedTypeMirror> typeArguments;

        public ZippedDeclaredType(ExtendedDeclaredType base, AnnotatedDeclaredType annotated) {
            super(base, annotated);
        }

        @Override
        public ExtendedDeclaredType getBase() {
            return (ExtendedDeclaredType)super.getBase();
        }

        @Override
        public AnnotatedDeclaredType getAnnotated() {
            return (AnnotatedDeclaredType)super.getAnnotated();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitDeclared(this, p);
        }

        @Override
        public Element asElement() {
            return getBase().asElement();
        }

        @Override
        public ExtendedTypeMirror getEnclosingType() {
            if (enclosingType == null) {
                enclosingType = zip(getBase().getEnclosingType(),
                        getAnnotated().getEnclosingType());
            }
            return enclosingType;
        }

        @Override
        public List<ExtendedTypeMirror> getTypeArguments() {
            if (typeArguments == null) {
                typeArguments = zipLists(getBase().getTypeArguments(),
                        getAnnotated().getTypeArguments());
            }
            return typeArguments;
        }
    }

    public static class ZippedExecutableType extends ZippedTypeMirror implements ExtendedExecutableType {
        private List<? extends ExtendedTypeMirror> parameterTypes;
        private ExtendedTypeMirror receiverType;
        private ExtendedTypeMirror returnType;
        private List<? extends ExtendedTypeMirror> thrownTypes;
        private List<? extends ExtendedTypeVariable> typeVariables;

        public ZippedExecutableType(ExtendedExecutableType base, AnnotatedExecutableType annotated) {
            super(base, annotated);
        }

        @Override
        public ExtendedExecutableType getBase() {
            return (ExtendedExecutableType)super.getBase();
        }

        @Override
        public AnnotatedExecutableType getAnnotated() {
            return (AnnotatedExecutableType)super.getAnnotated();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitExecutable(this, p);
        }

        @Override
        public ExecutableElement asElement() {
            return getBase().asElement();
        }

        @Override
        public List<? extends ExtendedTypeMirror> getParameterTypes() {
            if (parameterTypes == null) {
                parameterTypes = zipLists(getBase().getParameterTypes(),
                        getAnnotated().getParameterTypes());
            }
            return parameterTypes;
        }

        @Override
        public ExtendedTypeMirror getReceiverType() {
            if (receiverType == null) {
                receiverType = zip(getBase().getReceiverType(),
                        getAnnotated().getReceiverType());
            }
            return receiverType;
        }

        @Override
        public ExtendedTypeMirror getReturnType() {
            if (returnType == null) {
                returnType = zip(getBase().getReturnType(),
                        getAnnotated().getReturnType());
            }
            return returnType;
        }

        @Override
        public List<? extends ExtendedTypeMirror> getThrownTypes() {
            if (thrownTypes == null) {
                thrownTypes = zipLists(getBase().getThrownTypes(),
                        getAnnotated().getThrownTypes());
            }
            return thrownTypes;
        }

        @Override
        public List<? extends ExtendedTypeVariable> getTypeVariables() {
            if (typeVariables == null) {
                typeVariables = zipLists(getBase().getTypeVariables(),
                        getAnnotated().getTypeVariables());
            }
            return typeVariables;
        }
    }

    public static class ZippedIntersectionType extends ZippedTypeMirror implements ExtendedIntersectionType {
        private List<? extends ExtendedTypeMirror> bounds;

        public ZippedIntersectionType(ExtendedIntersectionType base, AnnotatedIntersectionType annotated) {
            super(base, annotated);
        }

        @Override
        public ExtendedIntersectionType getBase() {
            return (ExtendedIntersectionType)super.getBase();
        }

        @Override
        public AnnotatedIntersectionType getAnnotated() {
            return (AnnotatedIntersectionType)super.getAnnotated();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitIntersection(this, p);
        }

        @Override
        public List<? extends ExtendedTypeMirror> getBounds() {
            if (bounds == null) {
                bounds = zipLists(getBase().getBounds(),
                        getAnnotated().directSuperTypes());
            }
            return bounds;
        }
    }

    public static class ZippedNoType extends ZippedTypeMirror implements ExtendedNoType {
        public ZippedNoType(ExtendedNoType base, AnnotatedNoType annotated) {
            super(base, annotated);
        }

        @Override
        public ExtendedNoType getBase() {
            return (ExtendedNoType)super.getBase();
        }

        @Override
        public AnnotatedNoType getAnnotated() {
            return (AnnotatedNoType)super.getAnnotated();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitNoType(this, p);
        }
    }

    public static class ZippedNullType extends ZippedReferenceType implements ExtendedNullType {
        public ZippedNullType(ExtendedNullType base, AnnotatedNullType annotated) {
            super(base, annotated);
        }

        @Override
        public ExtendedNullType getBase() {
            return (ExtendedNullType)super.getBase();
        }

        @Override
        public AnnotatedNullType getAnnotated() {
            return (AnnotatedNullType)super.getAnnotated();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitNull(this, p);
        }
    }

    public static class ZippedPrimitiveType extends ZippedTypeMirror implements ExtendedPrimitiveType {
        public ZippedPrimitiveType(ExtendedPrimitiveType base, AnnotatedPrimitiveType annotated) {
            super(base, annotated);
        }

        @Override
        public ExtendedPrimitiveType getBase() {
            return (ExtendedPrimitiveType)super.getBase();
        }

        @Override
        public AnnotatedPrimitiveType getAnnotated() {
            return (AnnotatedPrimitiveType)super.getAnnotated();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitPrimitive(this, p);
        }
    }

    public static abstract class ZippedReferenceType extends ZippedTypeMirror implements ExtendedReferenceType {
        public ZippedReferenceType(ExtendedTypeMirror base, AnnotatedTypeMirror annotated) {
            super(base, annotated);
        }
    }

    public static class ZippedTypeVariable extends ZippedReferenceType implements ExtendedTypeVariable {
        private ExtendedTypeMirror lowerBound;
        private ExtendedTypeMirror upperBound;

        public ZippedTypeVariable(ExtendedTypeVariable base, AnnotatedTypeVariable annotated) {
            super(base, annotated);
        }

        @Override
        public ExtendedTypeVariable getBase() {
            return (ExtendedTypeVariable)super.getBase();
        }

        @Override
        public AnnotatedTypeVariable getAnnotated() {
            return (AnnotatedTypeVariable)super.getAnnotated();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitTypeVariable(this, p);
        }

        @Override
        public Element asElement() {
            return getBase().asElement();
        }

        @Override
        public ExtendedTypeMirror getLowerBound() {
            if (lowerBound == null) {
                lowerBound = zip(getBase().getLowerBound(),
                        getAnnotated().getLowerBound());
            }
            return lowerBound;
        }

        @Override
        public ExtendedTypeMirror getUpperBound() {
            if (upperBound == null) {
                upperBound = zip(getBase().getUpperBound(),
                        getAnnotated().getUpperBound());
            }
            return upperBound;
        }
    }

    public static class ZippedUnionType extends ZippedTypeMirror implements ExtendedUnionType {
        private List<? extends ExtendedTypeMirror> alternatives;

        public ZippedUnionType(ExtendedUnionType base, AnnotatedUnionType annotated) {
            super(base, annotated);
        }

        @Override
        public ExtendedUnionType getBase() {
            return (ExtendedUnionType)super.getBase();
        }

        @Override
        public AnnotatedUnionType getAnnotated() {
            return (AnnotatedUnionType)super.getAnnotated();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitUnion(this, p);
        }

        @Override
        public List<? extends ExtendedTypeMirror> getAlternatives() {
            if (alternatives == null) {
                alternatives = zipLists(getBase().getAlternatives(),
                        getAnnotated().getAlternatives());
            }
            return alternatives;
        }
    }

    public static class ZippedWildcardType extends ZippedTypeMirror implements ExtendedWildcardType {
        private ExtendedTypeMirror extendsBound;
        private ExtendedTypeMirror superBound;

        public ZippedWildcardType(ExtendedWildcardType base, AnnotatedWildcardType annotated) {
            super(base, annotated);
        }

        @Override
        public ExtendedWildcardType getBase() {
            return (ExtendedWildcardType)super.getBase();
        }

        @Override
        public AnnotatedWildcardType getAnnotated() {
            return (AnnotatedWildcardType)super.getAnnotated();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitWildcard(this, p);
        }

        @Override
        public ExtendedTypeMirror getExtendsBound() {
            if (extendsBound == null) {
                    extendsBound = zip(getBase().getExtendsBound(),
                            getAnnotated().getExtendsBound());
            }
            return extendsBound;
        }

        @Override
        public ExtendedTypeMirror getSuperBound() {
            if (superBound == null) {
                superBound = zip(getBase().getSuperBound(),
                        getAnnotated().getSuperBound());
            }
            return superBound;
        }
    }
}
