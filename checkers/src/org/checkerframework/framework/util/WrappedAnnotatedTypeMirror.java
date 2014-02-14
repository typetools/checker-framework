package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.ReferenceType;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.*;

// WARNING: Do not allow the underlying AnnotatedTypeMirror to be mutated
// while the WrappedAnnotatedTypeMirror is live!  Things will probably break!
public abstract class WrappedAnnotatedTypeMirror implements ExtendedTypeMirror {
    private AnnotatedTypeMirror raw;

    private WrappedAnnotatedTypeMirror(AnnotatedTypeMirror raw) {
        if (raw == null) {
            throw new IllegalArgumentException("raw ATM must be non-null");
        }
        this.raw = raw;
    }

    public static WrappedAnnotatedTypeMirror wrap(AnnotatedTypeMirror atm) {
        if (atm == null) {
            return null;
        }

        switch (atm.getKind()) {
            case ARRAY:
                return new WrappedAnnotatedArrayType((AnnotatedArrayType)atm);
            case DECLARED:
                return new WrappedAnnotatedDeclaredType((AnnotatedDeclaredType)atm);
            case EXECUTABLE:
                return new WrappedAnnotatedExecutableType((AnnotatedExecutableType)atm);
            case VOID:
            case PACKAGE:
            case NONE:
                return new WrappedAnnotatedNoType((AnnotatedNoType)atm);
            case NULL:
                return new WrappedAnnotatedNullType((AnnotatedNullType)atm);
            case TYPEVAR:
                return new WrappedAnnotatedTypeVariable((AnnotatedTypeVariable)atm);
            case WILDCARD:
                return new WrappedAnnotatedWildcardType((AnnotatedWildcardType)atm);
            case INTERSECTION:
                return new WrappedAnnotatedIntersectionType((AnnotatedIntersectionType)atm);
            case UNION:
                return new WrappedAnnotatedUnionType((AnnotatedUnionType)atm);
            default:
                if (atm.getKind().isPrimitive()) {
                    return new WrappedAnnotatedPrimitiveType((AnnotatedPrimitiveType)atm);
                }
                throw new IllegalArgumentException("unexpected type kind: " + atm.getKind());
        }
    }

    private static List<WrappedAnnotatedTypeMirror> wrapList(List<? extends AnnotatedTypeMirror> atms) {
        List<WrappedAnnotatedTypeMirror> watms = new ArrayList<>();
        for (AnnotatedTypeMirror atm : atms) {
            watms.add(wrap(atm));
        }
        return watms;
    }

    private static List<WrappedAnnotatedTypeVariable> wrapTypeVarList(List<? extends AnnotatedTypeVariable> atms) {
        List<WrappedAnnotatedTypeVariable> watms = new ArrayList<>();
        for (AnnotatedTypeVariable atm : atms) {
            watms.add((WrappedAnnotatedTypeVariable)wrap(atm));
        }
        return watms;
    }


    public AnnotatedTypeMirror unwrap() {
        return this.raw;
    }

    @Override public TypeMirror getRaw() {
        return raw.getUnderlyingType();
    }

    @Override
    public TypeKind getKind() {
        return raw.getKind();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        throw new UnsupportedOperationException("ATM doesn't support getAnnotation");
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        List<AnnotationMirror> result = new ArrayList<>();
        result.addAll(raw.getAnnotations());
        return result;
    }

    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        throw new UnsupportedOperationException("ATM doesn't support getAnnotationsByType");
    }

    @Override
    public String toString() {
        return raw.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        WrappedAnnotatedTypeMirror other = (WrappedAnnotatedTypeMirror)obj;
        return this.raw.equals(other.raw);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    static class WrappedAnnotatedArrayType extends WrappedAnnotatedReferenceType implements ExtendedArrayType {
        private WrappedAnnotatedTypeMirror componentType;

        public WrappedAnnotatedArrayType(AnnotatedArrayType raw) {
            super(raw);
            this.componentType = wrap(raw.getComponentType());
        }

        @Override @SuppressWarnings("unchecked")
        public ArrayType getRaw() {
            return (ArrayType)super.getRaw();
        }

        @Override
        public AnnotatedArrayType unwrap() {
            return (AnnotatedArrayType)super.unwrap();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitArray(this, p);
        }

        @Override
        public ExtendedTypeMirror getComponentType() {
            return componentType;
        }
    }

    static class WrappedAnnotatedDeclaredType extends WrappedAnnotatedReferenceType implements ExtendedDeclaredType {
        private WrappedAnnotatedTypeMirror enclosingType;
        private List<WrappedAnnotatedTypeMirror> typeArguments;

        public WrappedAnnotatedDeclaredType(AnnotatedDeclaredType raw) {
            super(raw);
            this.enclosingType = wrap(raw.getEnclosingType());
            this.typeArguments = wrapList(raw.getTypeArguments());
        }

        @Override @SuppressWarnings("unchecked")
        public DeclaredType getRaw() {
            return (DeclaredType)super.getRaw();
        }

        @Override
        public AnnotatedDeclaredType unwrap() {
            return (AnnotatedDeclaredType)super.unwrap();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitDeclared(this, p);
        }

        @Override
        public Element asElement() {
            return getRaw().asElement();
        }

        @Override
        public WrappedAnnotatedTypeMirror getEnclosingType() {
            return enclosingType;
        }

        @Override
        public List<? extends WrappedAnnotatedTypeMirror> getTypeArguments() {
            return typeArguments;
        }
    }

    static class WrappedAnnotatedExecutableType extends WrappedAnnotatedTypeMirror implements ExtendedExecutableType {
        private List<? extends ExtendedTypeMirror> parameterTypes;
        private ExtendedTypeMirror receiverType;
        private ExtendedTypeMirror returnType;
        private List<? extends ExtendedTypeMirror> thrownTypes;
        private List<? extends ExtendedTypeVariable> typeVariables;

        public WrappedAnnotatedExecutableType(AnnotatedExecutableType raw) {
            super(raw);
            this.parameterTypes = wrapList(raw.getParameterTypes());
            this.receiverType = wrap(raw.getReceiverType());
            this.returnType = wrap(raw.getReturnType());
            this.thrownTypes = wrapList(raw.getThrownTypes());
            this.typeVariables = wrapTypeVarList(raw.getTypeVariables());
        }

        @Override @SuppressWarnings("unchecked")
        public ExecutableType getRaw() {
            return (ExecutableType)super.getRaw();
        }

        @Override
        public AnnotatedExecutableType unwrap() {
            return (AnnotatedExecutableType)super.unwrap();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitExecutable(this, p);
        }

        @Override
        public ExecutableElement asElement() {
            return unwrap().getElement();
        }

        @Override
        public List<? extends ExtendedTypeMirror> getParameterTypes() {
            return parameterTypes;
        }

        @Override
        public ExtendedTypeMirror getReceiverType() {
            return receiverType;
        }

        @Override
        public ExtendedTypeMirror getReturnType() {
            return returnType;
        }

        @Override
        public List<? extends ExtendedTypeMirror> getThrownTypes() {
            return thrownTypes;
        }

        @Override
        public List<? extends ExtendedTypeVariable> getTypeVariables() {
            return typeVariables;
        }
    }

    static class WrappedAnnotatedIntersectionType extends WrappedAnnotatedTypeMirror implements ExtendedIntersectionType {
        private List<? extends ExtendedTypeMirror> bounds;

        public WrappedAnnotatedIntersectionType(AnnotatedIntersectionType raw) {
            super(raw);
            this.bounds = wrapList(raw.directSuperTypes());
        }

        @Override @SuppressWarnings("unchecked")
        public IntersectionType getRaw() {
            return (IntersectionType)super.getRaw();
        }

        @Override
        public AnnotatedIntersectionType unwrap() {
            return (AnnotatedIntersectionType)super.unwrap();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitIntersection(this, p);
        }

        @Override
        public List<? extends ExtendedTypeMirror> getBounds() {
            return bounds;
        }
    }

    static class WrappedAnnotatedNoType extends WrappedAnnotatedTypeMirror implements ExtendedNoType {
        public WrappedAnnotatedNoType(AnnotatedNoType raw) {
            super(raw);
        }

        @Override @SuppressWarnings("unchecked")
        public NoType getRaw() {
            return (NoType)super.getRaw();
        }

        @Override
        public AnnotatedNoType unwrap() {
            return (AnnotatedNoType)super.unwrap();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitNoType(this, p);
        }
    }

    static class WrappedAnnotatedNullType extends WrappedAnnotatedReferenceType implements ExtendedNullType {
        public WrappedAnnotatedNullType(AnnotatedNullType raw) {
            super(raw);
        }

        @Override @SuppressWarnings("unchecked")
        public NullType getRaw() {
            return (NullType)super.getRaw();
        }

        @Override
        public AnnotatedNullType unwrap() {
            return (AnnotatedNullType)super.unwrap();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitNull(this, p);
        }
    }

    static class WrappedAnnotatedPrimitiveType extends WrappedAnnotatedTypeMirror implements ExtendedPrimitiveType {
        public WrappedAnnotatedPrimitiveType(AnnotatedPrimitiveType raw) {
            super(raw);
        }

        @Override @SuppressWarnings("unchecked")
        public PrimitiveType getRaw() {
            return (PrimitiveType)super.getRaw();
        }

        @Override
        public AnnotatedPrimitiveType unwrap() {
            return (AnnotatedPrimitiveType)super.unwrap();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitPrimitive(this, p);
        }
    }

    static abstract class WrappedAnnotatedReferenceType extends WrappedAnnotatedTypeMirror implements ExtendedReferenceType {
        public WrappedAnnotatedReferenceType(AnnotatedTypeMirror raw) {
            super(raw);
        }
    }

    static class WrappedAnnotatedTypeVariable extends WrappedAnnotatedReferenceType implements ExtendedTypeVariable {
        private ExtendedTypeMirror lowerBound;
        private ExtendedTypeMirror upperBound;

        public WrappedAnnotatedTypeVariable(AnnotatedTypeVariable raw) {
            super(raw);
            this.lowerBound = wrap(raw.getLowerBound());
            this.upperBound = wrap(raw.getUpperBound());
        }

        @Override @SuppressWarnings("unchecked")
        public TypeVariable getRaw() {
            return (TypeVariable)super.getRaw();
        }

        @Override
        public AnnotatedTypeVariable unwrap() {
            return (AnnotatedTypeVariable)super.unwrap();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitTypeVariable(this, p);
        }

        @Override
        public Element asElement() {
            return getRaw().asElement();
        }

        @Override
        public ExtendedTypeMirror getLowerBound() {
            return lowerBound;
        }

        @Override
        public ExtendedTypeMirror getUpperBound() {
            return upperBound;
        }
    }

    static class WrappedAnnotatedUnionType extends WrappedAnnotatedTypeMirror implements ExtendedUnionType {
        private List<? extends ExtendedTypeMirror> alternatives;

        public WrappedAnnotatedUnionType(AnnotatedUnionType raw) {
            super(raw);
            this.alternatives = wrapList(raw.getAlternatives());
        }

        @Override @SuppressWarnings("unchecked")
        public UnionType getRaw() {
            return (UnionType)super.getRaw();
        }

        @Override
        public AnnotatedUnionType unwrap() {
            return (AnnotatedUnionType)super.unwrap();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitUnion(this, p);
        }

        @Override
        public List<? extends ExtendedTypeMirror> getAlternatives() {
            return alternatives;
        }
    }

    static class WrappedAnnotatedWildcardType extends WrappedAnnotatedTypeMirror implements ExtendedWildcardType {
        private ExtendedTypeMirror extendsBound;
        private ExtendedTypeMirror superBound;

        public WrappedAnnotatedWildcardType(AnnotatedWildcardType raw) {
            super(raw);
            this.extendsBound = wrap(raw.getExtendsBound());
            this.superBound = wrap(raw.getSuperBound());
        }

        @Override @SuppressWarnings("unchecked")
        public WildcardType getRaw() {
            return (WildcardType)super.getRaw();
        }

        @Override
        public AnnotatedWildcardType unwrap() {
            return (AnnotatedWildcardType)super.unwrap();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitWildcard(this, p);
        }

        @Override
        public ExtendedTypeMirror getExtendsBound() {
            return extendsBound;
        }

        @Override
        public ExtendedTypeMirror getSuperBound() {
            return superBound;
        }

        @Override
        public boolean equals(Object obj) {
            // AnnotatedWildcardType.equals is non-reflexive.  I hate everything.
            if (obj == null || obj.getClass() != this.getClass())
                return false;

            WrappedAnnotatedWildcardType other = (WrappedAnnotatedWildcardType)obj;
            return (this.extendsBound == null ?
                        other.extendsBound == null :
                        this.extendsBound.equals(other.extendsBound))
                && (this.superBound == null ?
                        other.superBound == null :
                        this.superBound.equals(other.superBound));
        }
    }
}
