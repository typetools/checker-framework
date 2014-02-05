package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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

public abstract class WrappedTypeMirror implements ExtendedTypeMirror {
    private TypeMirror raw;

    private WrappedTypeMirror(TypeMirror raw, WrappedTypeFactory factory) {
        this.raw = raw;
    }

    public TypeMirror getRaw() {
        return this.raw;
    }

    @Override
    public TypeKind getKind() {
        return raw.getKind();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return raw.getAnnotation(annotationType);
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return raw.getAnnotationMirrors();
    }

    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        return raw.getAnnotationsByType(annotationType);
    }

    static class WrappedArrayType extends WrappedReferenceType implements ExtendedArrayType {
        private WrappedTypeMirror componentType;
        public WrappedArrayType(ArrayType raw, WrappedTypeFactory factory) {
            super(raw, factory);
            this.componentType = factory.wrap(raw.getComponentType());
        }

        @Override @SuppressWarnings("unchecked")
        public ArrayType getRaw() {
            return (ArrayType)super.getRaw();
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

    static class WrappedDeclaredType extends WrappedReferenceType implements ExtendedDeclaredType {
        private WrappedTypeMirror enclosingType;
        private List<WrappedTypeMirror> typeArguments;

        public WrappedDeclaredType(DeclaredType raw, WrappedTypeFactory factory) {
            super(raw, factory);
            this.enclosingType = factory.wrap(raw.getEnclosingType());
            this.typeArguments = factory.wrap(raw.getTypeArguments());
        }

        @Override @SuppressWarnings("unchecked")
        public DeclaredType getRaw() {
            return (DeclaredType)super.getRaw();
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
        public WrappedTypeMirror getEnclosingType() {
            return enclosingType;
        }

        @Override
        public List<WrappedTypeMirror> getTypeArguments() {
            return typeArguments;
        }
    }

    static class WrappedErrorType extends WrappedDeclaredType implements ExtendedErrorType {
        public WrappedErrorType(ErrorType raw, WrappedTypeFactory factory) {
            super(raw, factory);
        }

        @Override @SuppressWarnings("unchecked")
        public ErrorType getRaw() {
            return (ErrorType)super.getRaw();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitError(this, p);
        }
    }

    static class WrappedExecutableType extends WrappedTypeMirror implements ExtendedExecutableType {
        private Element element;
        private List<? extends ExtendedTypeMirror> parameterTypes;
        private ExtendedTypeMirror receiverType;
        private ExtendedTypeMirror returnType;
        private List<? extends ExtendedTypeMirror> thrownTypes;
        private List<? extends ExtendedTypeVariable> typeVariables;

        public WrappedExecutableType(ExecutableType raw, Element element, WrappedTypeFactory factory) {
            super(raw, factory);
            this.element = element;
            this.parameterTypes = factory.wrap(raw.getParameterTypes());
            this.receiverType = factory.wrap(raw.getReceiverType());
            this.returnType = factory.wrap(raw.getReturnType());
            this.thrownTypes = factory.wrap(raw.getThrownTypes());
            this.typeVariables = factory.wrapTypeVariables(raw.getTypeVariables());

            if (this.element != null && this.element.getKind() == ElementKind.CONSTRUCTOR) {
                Element classElt = this.element.getEnclosingElement();
                assert classElt.getKind() == ElementKind.CLASS;
                this.returnType = factory.wrap(classElt.asType());
                // TODO: maybe need to do the same for this.receiverType
            }
        }

        @Override @SuppressWarnings("unchecked")
        public ExecutableType getRaw() {
            return (ExecutableType)super.getRaw();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitExecutable(this, p);
        }

        @Override
        public Element asElement() {
            return element;
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

    static class WrappedIntersectionType extends WrappedTypeMirror implements ExtendedIntersectionType {
        private List<? extends ExtendedTypeMirror> bounds;

        public WrappedIntersectionType(IntersectionType raw, WrappedTypeFactory factory) {
            super(raw, factory);
            this.bounds = factory.wrap(raw.getBounds());
        }

        @Override @SuppressWarnings("unchecked")
        public IntersectionType getRaw() {
            return (IntersectionType)super.getRaw();
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

    static class WrappedNoType extends WrappedTypeMirror implements ExtendedNoType {
        public WrappedNoType(NoType raw, WrappedTypeFactory factory) {
            super(raw, factory);
        }

        @Override @SuppressWarnings("unchecked")
        public NoType getRaw() {
            return (NoType)super.getRaw();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitNoType(this, p);
        }
    }

    static class WrappedNullType extends WrappedReferenceType implements ExtendedNullType {
        public WrappedNullType(NullType raw, WrappedTypeFactory factory) {
            super(raw, factory);
        }

        @Override @SuppressWarnings("unchecked")
        public NullType getRaw() {
            return (NullType)super.getRaw();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitNull(this, p);
        }
    }

    static class WrappedPrimitiveType extends WrappedTypeMirror implements ExtendedPrimitiveType {
        public WrappedPrimitiveType(PrimitiveType raw, WrappedTypeFactory factory) {
            super(raw, factory);
        }

        @Override @SuppressWarnings("unchecked")
        public PrimitiveType getRaw() {
            return (PrimitiveType)super.getRaw();
        }

        @Override
        public <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p) {
            return v.visitPrimitive(this, p);
        }
    }

    static abstract class WrappedReferenceType extends WrappedTypeMirror implements ExtendedReferenceType {
        public WrappedReferenceType(ReferenceType raw, WrappedTypeFactory factory) {
            super(raw, factory);
        }

        @Override @SuppressWarnings("unchecked")
        public ReferenceType getRaw() {
            return (ReferenceType)super.getRaw();
        }
    }

    static class WrappedTypeVariable extends WrappedReferenceType implements ExtendedTypeVariable {
        private ExtendedTypeMirror lowerBound;
        private ExtendedTypeMirror upperBound;

        public WrappedTypeVariable(TypeVariable raw, WrappedTypeFactory factory) {
            super(raw, factory);
            this.lowerBound = factory.wrap(raw.getLowerBound());
            this.upperBound = factory.wrap(raw.getUpperBound());
        }

        @Override @SuppressWarnings("unchecked")
        public TypeVariable getRaw() {
            return (TypeVariable)super.getRaw();
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

    static class WrappedUnionType extends WrappedTypeMirror implements ExtendedUnionType {
        private List<? extends ExtendedTypeMirror> alternatives;

        public WrappedUnionType(UnionType raw, WrappedTypeFactory factory) {
            super(raw, factory);
            this.alternatives = factory.wrap(raw.getAlternatives());
        }

        @Override @SuppressWarnings("unchecked")
        public UnionType getRaw() {
            return (UnionType)super.getRaw();
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

    static class WrappedWildcardType extends WrappedTypeMirror implements ExtendedWildcardType {
        private ExtendedTypeMirror extendsBound;
        private ExtendedTypeMirror superBound;

        public WrappedWildcardType(WildcardType raw, WrappedTypeFactory factory) {
            super(raw, factory);

            TypeMirror rawExtendsBound = raw.getExtendsBound();
            if (rawExtendsBound != null) {
                this.extendsBound = factory.wrap(rawExtendsBound);
            } else {
                this.extendsBound = factory.getWrappedObject();
            }

            TypeMirror rawSuperBound = raw.getSuperBound();
            if (rawSuperBound != null) {
                this.superBound = factory.wrap(rawSuperBound);
            } else {
                this.superBound = factory.getWrappedNull();
            }
        }

        @Override @SuppressWarnings("unchecked")
        public WildcardType getRaw() {
            return (WildcardType)super.getRaw();
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
    }
}
