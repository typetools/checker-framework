package org.checkerframework.qualframework.base;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;

import org.checkerframework.qualframework.util.ExtendedArrayType;
import org.checkerframework.qualframework.util.ExtendedDeclaredType;
import org.checkerframework.qualframework.util.ExtendedExecutableType;
import org.checkerframework.qualframework.util.ExtendedIntersectionType;
import org.checkerframework.qualframework.util.ExtendedNoType;
import org.checkerframework.qualframework.util.ExtendedNullType;
import org.checkerframework.qualframework.util.ExtendedPrimitiveType;
import org.checkerframework.qualframework.util.ExtendedTypeVariable;
import org.checkerframework.qualframework.util.ExtendedUnionType;
import org.checkerframework.qualframework.util.ExtendedWildcardType;
import org.checkerframework.qualframework.util.ExtendedTypeDeclaration;
import org.checkerframework.qualframework.util.ExtendedParameterDeclaration;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;

/**
 * A {@link ExtendedTypeMirror} with a qualifier for the top level and for
 * each component of the type.  For example, the qualified version of
 * <code>int[]</code> has one qualifier on the top-level {@link
 * QualifiedArrayType} and another on the {@link QualifiedPrimitiveType}
 * representing <code>int</code>.
 *
 * A {@link QualifiedTypeMirror} is immutable and always has exactly one
 * non-null qualifier.  In addition, the structure of the {@link
 * QualifiedTypeMirror} always matches the structure of the underlying {@link
 * ExtendedTypeMirror}.  That is, for any type with components (such as
 * <code>DeclaredType</code>, which has a list of type arguments as a
 * component), it will always be the case that
 * <code>qtm.getUnderlyingType().getComponent()</code> is equivalent to
 * <code>qtm.getComponent().getUnderlyingType()</code> according to
 * <code>Object.equals</code>.
 *
 * @see QualifiedTypeFactory
 */
public abstract class QualifiedTypeMirror<Q> {
    /** The underlying {@link ExtendedTypeMirror}. */
    private final ExtendedTypeMirror underlying;
    /** The qualifier in the main qualifier position of this type. */
    private final Q qualifier;

    private QualifiedTypeMirror(ExtendedTypeMirror underlying) {
        this.underlying = underlying;
        this.qualifier = null;
    }

    private QualifiedTypeMirror(ExtendedTypeMirror underlying, Q qualifier) {
        if (qualifier == null) {
            throw new IllegalArgumentException(
                    "cannot construct QualifiedTypeMirror with null qualifier");
        }

        this.underlying = underlying;
        this.qualifier = qualifier;
    }

    /** Applies a {@link QualifiedTypeVisitor} to this qualified type. */
    public abstract <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p);

    /** Gets the underlying {@link ExtendedTypeMirror}. */
    public ExtendedTypeMirror getUnderlyingType() {
        return underlying;
    }

    /** Gets the {@link TypeKind} of the underlying type. */
    public final TypeKind getKind() {
        return underlying.getKind();
    }

    /** Gets the qualifier in the main qualifier position of this type. */
    public final /* @NonNull*/ Q getQualifier() {
        return qualifier;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        QualifiedTypeMirror<Q> other = (QualifiedTypeMirror<Q>)obj;
        return this.qualifier.equals(other.qualifier)
            && this.underlying.equals(other.underlying);
    }

    @Override
    public int hashCode() {
        return this.qualifier.hashCode() * 17
            + this.underlying.hashCode() * 37;
    }


    /** Check that the underlying ExtendedTypeMirror has a specific TypeKind, and
     * throw an exception if it does not.  This is a helper method for
     * QualifiedTypeMirror subclass constructors.
     *
     * Note that we consider the {@code isDeclaration} field to be part of the
     * type kind.  In other words, we consider a {@code TYPEVAR} TypeMirror
     * with {@code isDeclaration} set to have kind {@code TYPEVAR DECLARATION},
     * which is distinct from ordinary {@code TYPEVAR}.
     */
    private static void checkUnderlyingKind(ExtendedTypeMirror underlying,
            TypeKind expectedKind, boolean expectedDeclaration) {
        TypeKind actualKind = underlying.getKind();
        boolean actualDeclaration = underlying.isDeclaration();
        if (actualKind != expectedKind || actualDeclaration != expectedDeclaration) {
            String actualKindName = actualKind + (actualDeclaration ? " DECLARATION" : "");
            String expectedKindName = expectedKind + (expectedDeclaration ? " DECLARATION" : "");
            throw new IllegalArgumentException(
                    "underlying ExtendedTypeMirror must have kind " + expectedKindName +
                    ", not " + actualKindName);
        }
    }

    private static void checkUnderlyingKind(ExtendedTypeMirror underlying, TypeKind expectedKind) {
        checkUnderlyingKind(underlying, expectedKind, false);
    }

    /** Check that the underlying ExtendedTypeMirror has one of the indicated
     * TypeKinds, and throw an exception if it does not.  This is a helper
     * method for QualifiedTypeMirror subclass constructors.
     *
     * This method requires the underlying type to have a non-{@code
     * DECLARATION} kind.  There is no flag to indicate that a declaration is
     * expected, unlike {@link checkUnderlyingKind}.
     */
    private static void checkUnderlyingKindIsOneOf(ExtendedTypeMirror underlying, TypeKind... validKinds) {
        TypeKind actualKind = underlying.getKind();

        if (underlying.isDeclaration()) {
            throw new IllegalArgumentException(
                    "underlying ExtendedTypeMirror must have one of the kinds " +
                    java.util.Arrays.toString(validKinds) + ", not " +
                    actualKind + " DECLARATION");
        }

        for (TypeKind kind : validKinds) {
            if (actualKind == kind) {
                // The ExtendedTypeMirror is valid.
                return;
            }
        }
        throw new IllegalArgumentException(
                "underlying ExtendedTypeMirror must have one of the kinds " +
                java.util.Arrays.toString(validKinds) + ", not " + actualKind);
    }

    /** Check that the underlying ExtendedTypeMirror has a primitive TypeKind, and
     * throw an exception if it does not.
     */
    // This method is here instead of in QualifiedPrimitiveType to keep it near
    // the other 'checkUnderlyingKind' methods.
    private static void checkUnderlyingKindIsPrimitive(ExtendedTypeMirror underlying) {
        TypeKind actualKind = underlying.getKind();
        if (!actualKind.isPrimitive()) {
            throw new IllegalArgumentException(
                    "underlying ExtendedTypeMirror must have primitive kind, not " + actualKind);
        }
    }


    /** Helper function to raise an appropriate exception in case of a mismatch
     * between qualified and unqualified versions of the same ExtendedTypeMirror.
     */
    private static <Q> void checkTypeMirrorsMatch(String description,
            QualifiedTypeMirror<Q> qualified, ExtendedTypeMirror unqualified) {
        if (!typeMirrorsMatch(qualified, unqualified)) {
            throw new IllegalArgumentException(
                    "qualified and unqualified " + description +
                    " TypeMirrors must be identical");
        }
    }

    /** Check if the underlying types of a list of QualifiedTypeMirrors match
     * the actual TypeMirrors from a second list.
     */
    private static <Q> void checkTypeMirrorListsMatch(String description,
            List<? extends QualifiedTypeMirror<Q>> qualified,
            List<? extends ExtendedTypeMirror> unqualified) {
        if (!typeMirrorListsMatch(qualified, unqualified)) {
            throw new IllegalArgumentException(
                    "qualified and unqualified " + description +
                    " TypeMirrors must be identical");
        }
    }

    /** Helper function for checkTypeMirrorsMatch.  Returns a boolean
     * indicating whether the qualified and unqualified types are
     * representations of the same type. */
    private static <Q> boolean typeMirrorsMatch(
            QualifiedTypeMirror<Q> qualified, ExtendedTypeMirror unqualified) {
        if (qualified == null && unqualified == null) {
            return true;
        }

        if (qualified == null || unqualified == null ||
                !qualified.getUnderlyingType().equals(unqualified)) {
            return false;
        }

        return true;
    }

    /** Helper function for checkTypeMirrorListsMatch.  Returns a boolean
     * indicating whether the qualified and unqualified lists have matching
     * TypeMirrors.
     */
    private static <Q> boolean typeMirrorListsMatch(
            List<? extends QualifiedTypeMirror<Q>> qualified,
            List<? extends ExtendedTypeMirror> unqualified) {
        if (qualified == null && unqualified == null) {
            return true;
        }
        if (qualified == null || unqualified == null) {
            return false;
        }
        if (unqualified.size() != qualified.size()) {
            return false;
        }

        for (int i = 0; i < qualified.size(); ++i) {
            if (!typeMirrorsMatch(qualified.get(i), unqualified.get(i))) {
                return false;
            }
        }

        return true;
    }


    /** Helper function for subclass toString methods.  Concatenates together
     * the results of calling toString on each of 'objs', with 'punct' between
     * each pair of elements. */
    private static String punctuatedList(String punct, List<? extends Object> objs) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object obj : objs) {
            if (!first) {
                sb.append(punct);
            } else {
                first = false;
            }
            sb.append(obj);
        }
        return sb.toString();
    }

    /** Shorthand for puncuatedList(", ", objs). */
    private static String commaSeparatedList(List<? extends Object> objs) {
        return punctuatedList(", ", objs);
    }


    public static final class QualifiedArrayType<Q> extends QualifiedTypeMirror<Q> {
        private final QualifiedTypeMirror<Q> componentType;

        public QualifiedArrayType(ExtendedTypeMirror underlying, Q qualifier,
                QualifiedTypeMirror<Q> componentType) {
            super(underlying, qualifier);
            checkUnderlyingKind(underlying, TypeKind.ARRAY);
            checkTypeMirrorsMatch("component",
                    componentType, getUnderlyingType().getComponentType());

            this.componentType = componentType;
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitArray(this, p);
        }

        @Override
        public ExtendedArrayType getUnderlyingType() {
            return (ExtendedArrayType)super.getUnderlyingType();
        }

        public QualifiedTypeMirror<Q> getComponentType() {
            return componentType;
        }

        @Override
        public String toString() {
            return getComponentType() + " " + getQualifier() + " []";
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            // super.equals ensures that 'obj.getClass() == this.getClass()'.
            @SuppressWarnings("unchecked")
            QualifiedArrayType<Q> other = (QualifiedArrayType<Q>)obj;
            return this.componentType.equals(other.componentType);
        }

        @Override
        public int hashCode() {
            return super.hashCode()
                + componentType.hashCode() * 43;
        }
    }


    public static final class QualifiedDeclaredType<Q> extends QualifiedTypeMirror<Q> {
        private final List<? extends QualifiedTypeMirror<Q>> typeArguments;

        public QualifiedDeclaredType(ExtendedTypeMirror underlying, Q qualifier,
                List<? extends QualifiedTypeMirror<Q>> typeArguments) {
            super(underlying, qualifier);
            checkUnderlyingKind(underlying, TypeKind.DECLARED);
            checkTypeMirrorListsMatch("argument",
                    typeArguments, getUnderlyingType().getTypeArguments());

            this.typeArguments = new ArrayList<>(typeArguments);
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitDeclared(this, p);
        }

        @Override
        public ExtendedDeclaredType getUnderlyingType() {
            return (ExtendedDeclaredType)super.getUnderlyingType();
        }

        public List<? extends QualifiedTypeMirror<Q>> getTypeArguments() {
            return typeArguments;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getQualifier());
            sb.append(" ");
            sb.append(getUnderlyingType());

            if (typeArguments.size() > 0) {
                sb.append("<").append(commaSeparatedList(typeArguments)).append(">");
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            // super.equals ensures that 'obj.getClass() == this.getClass()'.
            @SuppressWarnings("unchecked")
            QualifiedDeclaredType<Q> other = (QualifiedDeclaredType<Q>)obj;
            return this.typeArguments.equals(other.typeArguments);
        }

        @Override
        public int hashCode() {
            return super.hashCode()
                + typeArguments.hashCode() * 43;
        }
    }


    public static final class QualifiedExecutableType<Q> extends QualifiedTypeMirror<Q> {
        private final List<? extends QualifiedTypeMirror<Q>> parameterTypes;
        private final QualifiedTypeMirror<Q> receiverType;
        private final QualifiedTypeMirror<Q> returnType;
        private final List<? extends QualifiedTypeMirror<Q>> thrownTypes;
        private final List<? extends QualifiedParameterDeclaration<Q>> typeParameters;

        public QualifiedExecutableType(ExtendedTypeMirror underlying,
                List<? extends QualifiedTypeMirror<Q>> parameterTypes,
                QualifiedTypeMirror<Q> receiverType,
                QualifiedTypeMirror<Q> returnType,
                List<? extends QualifiedTypeMirror<Q>> thrownTypes,
                List<? extends QualifiedParameterDeclaration<Q>> typeParameters) {
            super(underlying);
            checkUnderlyingKind(underlying, TypeKind.EXECUTABLE);
            checkTypeMirrorListsMatch("parameter",
                    parameterTypes, getUnderlyingType().getParameterTypes());
            checkTypeMirrorsMatch("receiver",
                    receiverType, getUnderlyingType().getReceiverType());
            checkTypeMirrorsMatch("return",
                    returnType, getUnderlyingType().getReturnType());
            checkTypeMirrorListsMatch("thrown",
                    thrownTypes, getUnderlyingType().getThrownTypes());
            checkTypeMirrorListsMatch("type parameter",
                    typeParameters, getUnderlyingType().getTypeParameters());

            this.parameterTypes = new ArrayList<>(parameterTypes);
            this.receiverType = receiverType;
            this.returnType = returnType;
            this.thrownTypes = new ArrayList<>(thrownTypes);
            this.typeParameters = new ArrayList<>(typeParameters);
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitExecutable(this, p);
        }

        @Override
        public ExtendedExecutableType getUnderlyingType() {
            return (ExtendedExecutableType)super.getUnderlyingType();
        }

        public List<? extends QualifiedTypeMirror<Q>> getParameterTypes() {
            return parameterTypes;
        }

        public QualifiedTypeMirror<Q> getReceiverType() {
            return receiverType;
        }

        public QualifiedTypeMirror<Q> getReturnType() {
            return returnType;
        }

        public List<? extends QualifiedTypeMirror<Q>> getThrownTypes() {
            return thrownTypes;
        }

        public List<? extends QualifiedParameterDeclaration<Q>> getTypeParameters() {
            return typeParameters;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getQualifier()).append(" : ");

            if (typeParameters.size() > 0) {
                sb.append("<").append(commaSeparatedList(typeParameters)).append(">");
            }

            sb.append(returnType);

            sb.append("((");
            if (receiverType != null) {
                sb.append(receiverType).append(" this, ");
            }
            sb.append(commaSeparatedList(parameterTypes));
            sb.append("))");

            if (thrownTypes.size() > 0) {
                sb.append(" throws ").append(commaSeparatedList(thrownTypes));
            }

            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            // super.equals ensures that 'obj.getClass() == this.getClass()'.
            @SuppressWarnings("unchecked")
            QualifiedExecutableType<Q> other = (QualifiedExecutableType<Q>)obj;
            return this.parameterTypes.equals(other.parameterTypes)
                && (this.receiverType == null ?
                        other.receiverType == null :
                        this.receiverType.equals(other.receiverType))
                && (this.returnType == null ?
                        other.returnType == null :
                        this.returnType.equals(other.returnType))
                && this.thrownTypes.equals(other.thrownTypes)
                && this.typeParameters.equals(other.typeParameters);
        }

        @Override
        public int hashCode() {
            return super.hashCode()
                + parameterTypes.hashCode() * 43
                + (this.receiverType == null ? 0 : this.receiverType.hashCode() * 67)
                + (this.returnType == null ? 0 : this.returnType.hashCode() * 83)
                + thrownTypes.hashCode() * 109
                + typeParameters.hashCode() * 127;
        }
    }

    public static final class QualifiedIntersectionType<Q> extends QualifiedTypeMirror<Q> {
        private final List<? extends QualifiedTypeMirror<Q>> bounds;

        public QualifiedIntersectionType(ExtendedTypeMirror underlying, Q qualifier,
                List<? extends QualifiedTypeMirror<Q>> bounds) {
            super(underlying, qualifier);
            checkUnderlyingKind(underlying, TypeKind.INTERSECTION);
            checkTypeMirrorListsMatch("bounds",
                    bounds, getUnderlyingType().getBounds());

            this.bounds = new ArrayList<>(bounds);
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitIntersection(this, p);
        }

        @Override
        public ExtendedIntersectionType getUnderlyingType() {
            return (ExtendedIntersectionType)super.getUnderlyingType();
        }

        public List<? extends QualifiedTypeMirror<Q>> getBounds() {
            return bounds;
        }

        @Override
        public String toString() {
            return punctuatedList(" & ", bounds);
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            // super.equals ensures that 'obj.getClass() == this.getClass()'.
            @SuppressWarnings("unchecked")
            QualifiedIntersectionType<Q> other = (QualifiedIntersectionType<Q>)obj;
            return this.bounds.equals(other.bounds);
        }

        @Override
        public int hashCode() {
            return super.hashCode()
                + bounds.hashCode() * 43;
        }
    }

    public static final class QualifiedNoType<Q> extends QualifiedTypeMirror<Q> {
        public QualifiedNoType(ExtendedTypeMirror underlying, Q qualifier) {
            super(underlying, qualifier);
            // According to the ExtendedNoType javadocs, valid kinds are NONE, PACKAGE,
            // and VOID.
            checkUnderlyingKindIsOneOf(underlying,
                    TypeKind.NONE, TypeKind.PACKAGE, TypeKind.VOID);
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitNoType(this, p);
        }

        @Override
        public ExtendedNoType getUnderlyingType() {
            return (ExtendedNoType)super.getUnderlyingType();
        }

        @Override
        public String toString() {
            return getQualifier() + " " + getUnderlyingType();
        }

        // Use superclass implementation of 'equals' and 'hashCode'.
    }

    public static final class QualifiedNullType<Q> extends QualifiedTypeMirror<Q> {
        public QualifiedNullType(ExtendedTypeMirror underlying, Q qualifier) {
            super(underlying, qualifier);
            checkUnderlyingKind(underlying, TypeKind.NULL);
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitNull(this, p);
        }

        @Override
        public ExtendedNullType getUnderlyingType() {
            return (ExtendedNullType)super.getUnderlyingType();
        }

        @Override
        public String toString() {
            return getQualifier() + " " + getUnderlyingType();
        }

        // Use superclass implementation of 'equals' and 'hashCode'.
    }

    public static final class QualifiedPrimitiveType<Q> extends QualifiedTypeMirror<Q> {
        public QualifiedPrimitiveType(ExtendedTypeMirror underlying, Q qualifier) {
            super(underlying, qualifier);
            checkUnderlyingKindIsPrimitive(underlying);
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitPrimitive(this, p);
        }

        @Override
        public ExtendedPrimitiveType getUnderlyingType() {
            return (ExtendedPrimitiveType)super.getUnderlyingType();
        }

        @Override
        public String toString() {
            return getQualifier() + " " + getUnderlyingType();
        }

        // Use superclass implementation of 'equals' and 'hashCode'.
    }

    // There is no QualifiedReferenceType.  If we really need one, we can add
    // it to the hierarchy as an empty abstract class between
    // QualifiedTypeMirror and the qualified reference types (ExtendedArrayType,
    // ExtendedDeclaredType, ExtendedNullType, ExtendedTypeVariable).

    public static final class QualifiedTypeVariable<Q> extends QualifiedTypeMirror<Q> {
        public QualifiedTypeVariable(ExtendedTypeMirror underlying, Q qualifier) {
            super(underlying, qualifier);
            checkUnderlyingKind(underlying, TypeKind.TYPEVAR);
            if (getUnderlyingType().asElement().getKind() != ElementKind.TYPE_PARAMETER) {
                throw new IllegalArgumentException(
                        "underlying type's asElement() must have kind TYPE_PARAMETER");
            }
        }

        /**
         * @return if the primary qualifier in this QualifiedTypeVariable is valid
         *      and should be used.
         */
        public boolean isPrimaryQualifierValid() {
            return getUnderlyingType().getAnnotationMirrors().size() > 0;
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitTypeVariable(this, p);
        }

        @Override
        public ExtendedTypeVariable getUnderlyingType() {
            return (ExtendedTypeVariable)super.getUnderlyingType();
        }

        public TypeParameterElement asElement() {
            return (TypeParameterElement)getUnderlyingType().asElement();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getQualifier()).append(" ")
                    .append(getUnderlyingType().asElement().getSimpleName());
            return sb.toString();
        }

        // Use superclass 'equals' and 'hashCode'

        public QualifiedParameterDeclaration<Q> getDeclaration() {
            return new QualifiedParameterDeclaration<>(getUnderlyingType().getDeclaration());
        }
    }

    public static final class QualifiedUnionType<Q> extends QualifiedTypeMirror<Q> {
        private final List<? extends QualifiedTypeMirror<Q>> alternatives;

        public QualifiedUnionType(ExtendedTypeMirror underlying, Q qualifier,
                List<? extends QualifiedTypeMirror<Q>> alternatives) {
            super(underlying, qualifier);
            checkUnderlyingKind(underlying, TypeKind.UNION);
            checkTypeMirrorListsMatch("alternative",
                    alternatives, getUnderlyingType().getAlternatives());

            this.alternatives = new ArrayList<>(alternatives);
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitUnion(this, p);
        }

        @Override
        public ExtendedUnionType getUnderlyingType() {
            return (ExtendedUnionType)super.getUnderlyingType();
        }

        public List<? extends QualifiedTypeMirror<Q>> getAlternatives() {
            return alternatives;
        }

        @Override
        public String toString() {
            return punctuatedList(" | ", alternatives);
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            // super.equals ensures that 'obj.getClass() == this.getClass()'.
            @SuppressWarnings("unchecked")
            QualifiedUnionType<Q> other = (QualifiedUnionType<Q>)obj;
            return this.alternatives.equals(other.alternatives);
        }

        @Override
        public int hashCode() {
            return super.hashCode()
                + alternatives.hashCode() * 43;
        }
    }

    public static final class QualifiedWildcardType<Q> extends QualifiedTypeMirror<Q> {
        private final QualifiedTypeMirror<Q> extendsBound;
        private final QualifiedTypeMirror<Q> superBound;

        public QualifiedWildcardType(ExtendedTypeMirror underlying,
                QualifiedTypeMirror<Q> extendsBound,
                QualifiedTypeMirror<Q> superBound) {
            super(underlying);
            checkUnderlyingKind(underlying, TypeKind.WILDCARD);
            checkTypeMirrorsMatch("extends bound",
                        extendsBound, getUnderlyingType().getExtendsBound());
            checkTypeMirrorsMatch("super bound",
                    superBound, getUnderlyingType().getSuperBound());

            this.extendsBound = extendsBound;
            this.superBound = superBound;
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitWildcard(this, p);
        }

        @Override
        public ExtendedWildcardType getUnderlyingType() {
            return (ExtendedWildcardType)super.getUnderlyingType();
        }

        public QualifiedTypeMirror<Q> getExtendsBound() {
            return extendsBound;
        }

        public QualifiedTypeMirror<Q> getSuperBound() {
            return superBound;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getQualifier()).append(" ?")
                    .append(" extends ").append(extendsBound)
                    .append(" super ").append(superBound);
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            // super.equals ensures that 'obj.getClass() == this.getClass()'.
            @SuppressWarnings("unchecked")
            QualifiedWildcardType<Q> other = (QualifiedWildcardType<Q>)obj;
            return this.extendsBound.equals(other.extendsBound)
                && (this.superBound == null ?
                        other.superBound == null :
                        this.superBound.equals(other.superBound));
        }

        @Override
        public int hashCode() {
            return super.hashCode()
                + extendsBound.hashCode() * 43
                + (superBound == null ? 0 : superBound.hashCode() * 67);
        }
    }


    public static final class QualifiedTypeDeclaration<Q> extends QualifiedTypeMirror<Q> {
        private final List<? extends QualifiedParameterDeclaration<Q>> typeParameters;

        public QualifiedTypeDeclaration(ExtendedTypeMirror underlying, Q qualifier,
                List<? extends QualifiedParameterDeclaration<Q>> typeParameters) {
            super(underlying, qualifier);
            checkUnderlyingKind(underlying, TypeKind.DECLARED, true);
            checkTypeMirrorListsMatch("parameter",
                    typeParameters, getUnderlyingType().getTypeParameters());

            this.typeParameters = new ArrayList<>(typeParameters);
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitTypeDeclaration(this, p);
        }

        @Override
        public ExtendedTypeDeclaration getUnderlyingType() {
            return (ExtendedTypeDeclaration)super.getUnderlyingType();
        }

        public List<? extends QualifiedParameterDeclaration<Q>> getTypeParameters() {
            return typeParameters;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getQualifier());
            sb.append(" ");
            sb.append(getUnderlyingType());

            if (typeParameters.size() > 0) {
                sb.append("<").append(commaSeparatedList(typeParameters)).append(">");
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            // super.equals ensures that 'obj.getClass() == this.getClass()'.
            @SuppressWarnings("unchecked")
            QualifiedTypeDeclaration<Q> other = (QualifiedTypeDeclaration<Q>)obj;
            return this.typeParameters.equals(other.typeParameters);
        }

        @Override
        public int hashCode() {
            return super.hashCode()
                + typeParameters.hashCode() * 43;
        }
    }


    public static final class QualifiedParameterDeclaration<Q> extends QualifiedTypeMirror<Q> {
        // This class has no fields.  Its upper and lower bounds are stored in
        // the QTF's symbol table.

        public QualifiedParameterDeclaration(ExtendedTypeMirror underlying) {
            super(underlying);
            checkUnderlyingKind(underlying, TypeKind.TYPEVAR, true);
            if (getUnderlyingType().asElement().getKind() != ElementKind.TYPE_PARAMETER) {
                throw new IllegalArgumentException(
                        "underlying type's asElement() must have kind TYPE_PARAMETER");
            }
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitParameterDeclaration(this, p);
        }

        @Override
        public ExtendedParameterDeclaration getUnderlyingType() {
            return (ExtendedParameterDeclaration)super.getUnderlyingType();
        }

        public TypeParameterElement asElement() {
            return (TypeParameterElement)getUnderlyingType().asElement();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getQualifier()).append(" ")
                    .append(getUnderlyingType().asElement().getSimpleName());
            return sb.toString();
        }

        // Use superclass 'equals' and 'hashCode'
    }
}
