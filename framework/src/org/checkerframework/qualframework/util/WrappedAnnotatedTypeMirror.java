package org.checkerframework.qualframework.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.IdentityHashMap;
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

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.*;
import org.checkerframework.framework.util.AnnotatedTypes;

/**
 * A wrapper to adapt an {@link AnnotatedTypeMirror} to the {@link
 * ExtendedTypeMirror} interface.  Instances of this class are immutable.
 */
public abstract class WrappedAnnotatedTypeMirror implements ExtendedTypeMirror {
    private AnnotatedTypeMirror raw;

    /**
     * Helper class for the {@link wrap} method.
     */
    // TODO: This used to be necessary to keep track of previously visited type
    // variables, but I think the new type variable handling makes it
    // unnecessary.  Pull the methods in this class out as static methods of
    // WATM, and remove the 'Factory' argument of the WATM constructors.
    private static class Factory {
        public WrappedAnnotatedTypeMirror wrap(AnnotatedTypeMirror atm) {
            if (atm == null) {
                return null;
            }

            switch (atm.getKind()) {
                case ARRAY:
                    return new WrappedAnnotatedArrayType((AnnotatedArrayType)atm, this);
                case DECLARED:
                    return new WrappedAnnotatedDeclaredType((AnnotatedDeclaredType)atm, this);
                case EXECUTABLE:
                    return new WrappedAnnotatedExecutableType((AnnotatedExecutableType)atm, this);
                case VOID:
                case PACKAGE:
                case NONE:
                    return new WrappedAnnotatedNoType((AnnotatedNoType)atm, this);
                case NULL:
                    return new WrappedAnnotatedNullType((AnnotatedNullType)atm, this);
                case TYPEVAR:
                    return new WrappedAnnotatedTypeVariable((AnnotatedTypeVariable)atm, this);
                case WILDCARD:
                    return new WrappedAnnotatedWildcardType((AnnotatedWildcardType)atm, this);
                case INTERSECTION:
                    return new WrappedAnnotatedIntersectionType((AnnotatedIntersectionType)atm, this);
                case UNION:
                    return new WrappedAnnotatedUnionType((AnnotatedUnionType)atm, this);
                default:
                    if (atm.getKind().isPrimitive()) {
                        return new WrappedAnnotatedPrimitiveType((AnnotatedPrimitiveType)atm, this);
                    }
                    throw new IllegalArgumentException("unexpected type kind: " + atm.getKind());
            }
        }

        public List<WrappedAnnotatedTypeMirror> wrapList(List<? extends AnnotatedTypeMirror> atms) {
            List<WrappedAnnotatedTypeMirror> watms = new ArrayList<>();
            for (AnnotatedTypeMirror atm : atms) {
                watms.add(wrap(atm));
            }
            return watms;
        }

        public List<WrappedAnnotatedTypeVariable> wrapTypeVarList(List<? extends AnnotatedTypeVariable> atms) {
            List<WrappedAnnotatedTypeVariable> watms = new ArrayList<>();
            for (AnnotatedTypeVariable atm : atms) {
                watms.add((WrappedAnnotatedTypeVariable)wrap(atm));
            }
            return watms;
        }
    }

    private WrappedAnnotatedTypeMirror(AnnotatedTypeMirror raw) {
        if (raw == null) {
            throw new IllegalArgumentException("raw ATM must be non-null");
        }
        this.raw = raw;
    }

    /**
     * Constructs a {@link WrappedAnnotatedTypeMirror} from an {@link
     * AnnotatedTypeMirror}.  The {@link WrappedAnnotatedTypeMirror} returned
     * by this method will be backed by a deep copy of the input
     * {@link AnnotatedTypeMirror}, so later mutations of the input will not
     * affect the wrapped version.
     */
    public static WrappedAnnotatedTypeMirror wrap(AnnotatedTypeMirror atm) {
        // TODO: Uh oh... something is broken in TypeMirrorConverter.  Using
        // 'deepCopy' here (which is necessary to make WATM instances actually
        // be immutable) results in lots of "cannot construct
        // QualifiedTypeMirror with null qualifier" errors.

        //return new Factory().wrap(AnnotatedTypes.deepCopy(atm));
        return new Factory().wrap(atm);
    }

    /**
     * Unwrap a {@link WrappedAnnotatedTypeMirror} to obtain the original
     * {@link AnnotatedTypeMirror}.
     */
    public AnnotatedTypeMirror unwrap() {
        return this.raw;
    }

    @Override
    public TypeMirror getRaw() {
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
        WrappedAnnotatedTypeMirror other = (WrappedAnnotatedTypeMirror)obj;
        return this.raw.equals(other.raw);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    public static class WrappedAnnotatedArrayType extends WrappedAnnotatedReferenceType implements ExtendedArrayType {
        private WrappedAnnotatedTypeMirror componentType;

        private WrappedAnnotatedArrayType(AnnotatedArrayType raw, Factory factory) {
            super(raw);
            this.componentType = factory.wrap(raw.getComponentType());
        }

        @Override
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
        public WrappedAnnotatedTypeMirror getComponentType() {
            return componentType;
        }
    }

    public static class WrappedAnnotatedDeclaredType extends WrappedAnnotatedReferenceType implements ExtendedDeclaredType {
        private WrappedAnnotatedTypeMirror enclosingType;
        private List<WrappedAnnotatedTypeMirror> typeArguments;

        private WrappedAnnotatedDeclaredType(AnnotatedDeclaredType raw, Factory factory) {
            super(raw);
            this.enclosingType = factory.wrap(raw.getEnclosingType());
            this.typeArguments = factory.wrapList(raw.getTypeArguments());
        }

        @Override
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

    public static class WrappedAnnotatedExecutableType extends WrappedAnnotatedTypeMirror implements ExtendedExecutableType {
        private List<? extends WrappedAnnotatedTypeMirror> parameterTypes;
        private WrappedAnnotatedTypeMirror receiverType;
        private WrappedAnnotatedTypeMirror returnType;
        private List<? extends WrappedAnnotatedTypeMirror> thrownTypes;
        private List<? extends WrappedAnnotatedTypeVariable> typeVariables;

        private WrappedAnnotatedExecutableType(AnnotatedExecutableType raw, Factory factory) {
            super(raw);
            this.parameterTypes = factory.wrapList(raw.getParameterTypes());
            this.receiverType = factory.wrap(raw.getReceiverType());
            this.returnType = factory.wrap(raw.getReturnType());
            this.thrownTypes = factory.wrapList(raw.getThrownTypes());
            this.typeVariables = factory.wrapTypeVarList(raw.getTypeVariables());
        }

        @Override
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
        public WrappedAnnotatedTypeMirror getReceiverType() {
            return receiverType;
        }

        @Override
        public WrappedAnnotatedTypeMirror getReturnType() {
            return returnType;
        }

        @Override
        public List<? extends WrappedAnnotatedTypeMirror> getThrownTypes() {
            return thrownTypes;
        }

        @Override
        public List<? extends WrappedAnnotatedTypeVariable> getTypeVariables() {
            return typeVariables;
        }
    }

    public static class WrappedAnnotatedIntersectionType extends WrappedAnnotatedTypeMirror implements ExtendedIntersectionType {
        private List<? extends WrappedAnnotatedTypeMirror> bounds;

        private WrappedAnnotatedIntersectionType(AnnotatedIntersectionType raw, Factory factory) {
            super(raw);
            this.bounds = factory.wrapList(raw.directSuperTypes());
        }

        @Override
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
        public List<? extends WrappedAnnotatedTypeMirror> getBounds() {
            return bounds;
        }
    }

    public static class WrappedAnnotatedNoType extends WrappedAnnotatedTypeMirror implements ExtendedNoType {
        private WrappedAnnotatedNoType(AnnotatedNoType raw, Factory factory) {
            super(raw);
        }

        @Override
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

    public static class WrappedAnnotatedNullType extends WrappedAnnotatedReferenceType implements ExtendedNullType {
        private WrappedAnnotatedNullType(AnnotatedNullType raw, Factory factory) {
            super(raw);
        }

        @Override
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

    public static class WrappedAnnotatedPrimitiveType extends WrappedAnnotatedTypeMirror implements ExtendedPrimitiveType {
        private WrappedAnnotatedPrimitiveType(AnnotatedPrimitiveType raw, Factory factory) {
            super(raw);
        }

        @Override
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
        private WrappedAnnotatedReferenceType(AnnotatedTypeMirror raw) {
            super(raw);
        }
    }

    public static class WrappedAnnotatedTypeVariable extends WrappedAnnotatedReferenceType implements ExtendedTypeVariable {
        private WrappedAnnotatedTypeVariable(AnnotatedTypeVariable raw, Factory factory) {
            super(raw);
        }

        @Override
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
    }

    public static class WrappedAnnotatedUnionType extends WrappedAnnotatedTypeMirror implements ExtendedUnionType {
        private List<? extends WrappedAnnotatedTypeMirror> alternatives;

        private WrappedAnnotatedUnionType(AnnotatedUnionType raw, Factory factory) {
            super(raw);
            this.alternatives = factory.wrapList(raw.getAlternatives());
        }

        @Override
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
        public List<? extends WrappedAnnotatedTypeMirror> getAlternatives() {
            return alternatives;
        }
    }

    public static class WrappedAnnotatedWildcardType extends WrappedAnnotatedTypeMirror implements ExtendedWildcardType {
        private WrappedAnnotatedTypeMirror extendsBound;
        private WrappedAnnotatedTypeMirror superBound;

        private WrappedAnnotatedWildcardType(AnnotatedWildcardType raw, Factory factory) {
            super(raw);
            this.extendsBound = factory.wrap(raw.getExtendsBound());
            this.superBound = factory.wrap(raw.getSuperBound());
        }

        @Override
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
        public WrappedAnnotatedTypeMirror getExtendsBound() {
            return extendsBound;
        }

        @Override
        public WrappedAnnotatedTypeMirror getSuperBound() {
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
