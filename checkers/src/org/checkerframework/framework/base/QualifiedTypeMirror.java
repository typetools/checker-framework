package org.checkerframework.framework.base;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
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
import javax.lang.model.util.Types;

public abstract class QualifiedTypeMirror<Q> {
    private final TypeMirror underlying;
    private final Q qualifier;

    private QualifiedTypeMirror(TypeMirror underlying, Q qualifier) {
        if (qualifier == null) {
            throw new IllegalArgumentException(
                    "cannot construct QualifiedTypeMirror with null qualifier");
        }

        this.underlying = underlying;
        this.qualifier = qualifier;
    }

    public abstract <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p);

    public TypeMirror getUnderlyingType() {
        return underlying;
    }

    public final TypeKind getKind() {
        return underlying.getKind();
    }

    public final /*@NonNull*/ Q getQualifier() {
        return qualifier;
    }

    public Q getEffectiveQualifier() {
        return qualifier;
    }

    public String toString() {
        return qualifier + " " + underlying;
    }


    /** Check that the underlying TypeMirror has a specific TypeKind, and
     * throw an exception if it does not.  This is a helper method for
     * QualifiedTypeMirror subclass constructors.
     */
    private static void checkUnderlyingKind(TypeMirror underlying, TypeKind expectedKind) {
        TypeKind actualKind = underlying.getKind();
        if (actualKind != expectedKind) {
            throw new IllegalArgumentException(
                    "underlying TypeMirror must have kind " + expectedKind +
                    ", not " + actualKind);
        }
    }

    /** Check that the underlying TypeMirror has one of the indicated
     * TypeKinds, and throw an exception if it does not.  This is a helper
     * method for QualifiedTypeMirror subclass constructors.
     */
    private static void checkUnderlyingKindIsOneOf(TypeMirror underlying, TypeKind... validKinds) {
        TypeKind actualKind = underlying.getKind();
        for (TypeKind kind : validKinds) {
            if (actualKind == kind) {
                // The TypeMirror is valid.
                return;
            }
        }
        throw new IllegalArgumentException(
                "underlying TypeMirror must have one of the kinds " +
                java.util.Arrays.toString(validKinds) + ", not " + actualKind);
    }

    /** Check that the underlying TypeMirror has a primitive TypeKind, and
     * throw an exception if it does not.
     */
    // This method is here instead of in QualifiedPrimitiveType so that its
    // exception message can be kept consistent with the message from
    // checkUnderlyingKind.
    private static void checkUnderlyingKindIsPrimitive(TypeMirror underlying) {
        TypeKind actualKind = underlying.getKind();
        if (!actualKind.isPrimitive()) {
            throw new IllegalArgumentException(
                    "underlying TypeMirror must have primitive kind, not " + actualKind);
        }
    }

    /** Helper function to raise an appropriate exception in case of a mismatch
     * between qualified and unqualified versions of the same TypeMirror.
     */
    private static <Q> void checkTypeMirrorsMatch(String description,
            QualifiedTypeMirror<Q> qualified, TypeMirror unqualified) {
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
            List<? extends TypeMirror> unqualified) {
        if (!typeMirrorListsMatch(qualified, unqualified)) {
            throw new IllegalArgumentException(
                    "qualified and unqualified " + description +
                    " TypeMirrors must be identical");
        }
    }

    private static <Q> boolean typeMirrorsMatch(
            QualifiedTypeMirror<Q> qualified, TypeMirror unqualified) {
        if (qualified == null && unqualified == null) {
            return true;
        }

        if (qualified == null || unqualified == null ||
                qualified.getUnderlyingType() != unqualified) {
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
            List<? extends TypeMirror> unqualified) {
        if (qualified == null && unqualified == null) {
            return true;
        }
        if (qualified == null || unqualified == null) {
            return false;
        }
        if (unqualified.size() != qualified.size())
            return false;

        for (int i = 0; i < qualified.size(); ++i) {
            if (!typeMirrorsMatch(qualified.get(i), unqualified.get(i))) {
                return false;
            }
        }

        return true;
    }

    private static String commaSeparatedList(List<? extends Object> objs) {
        return punctuatedList(", ", objs);
    }

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


    public static final class QualifiedArrayType<Q> extends QualifiedTypeMirror<Q> {
        private final QualifiedTypeMirror<Q> componentType;

        public QualifiedArrayType(TypeMirror underlying, Q qualifier,
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

        public ArrayType getUnderlyingType() {
            return (ArrayType)super.getUnderlyingType();
        }

        public QualifiedTypeMirror<Q> getComponentType() {
            return componentType;
        }

        public String toString() {
            return getComponentType() + " " + getQualifier() + " []";
        }
    }

    
    public static final class QualifiedDeclaredType<Q> extends QualifiedTypeMirror<Q> {
        private final List<? extends QualifiedTypeMirror<Q>> typeArguments;

        public QualifiedDeclaredType(TypeMirror underlying, Q qualifier,
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

        public DeclaredType getUnderlyingType() {
            return (DeclaredType)super.getUnderlyingType();
        }

        public List<? extends QualifiedTypeMirror<Q>> getTypeArguments() {
            return typeArguments;
        }

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
    }


    public static final class QualifiedExecutableType<Q> extends QualifiedTypeMirror<Q> {
        private final List<? extends QualifiedTypeMirror<Q>> parameterTypes;
        private final QualifiedTypeMirror<Q> receiverType;
        private final QualifiedTypeMirror<Q> returnType;
        private final List<? extends QualifiedTypeMirror<Q>> thrownTypes;
        private final List<? extends QualifiedTypeVariable<Q>> typeVariables;

        public QualifiedExecutableType(TypeMirror underlying, Q qualifier,
                List<? extends QualifiedTypeMirror<Q>> parameterTypes,
                QualifiedTypeMirror<Q> receiverType,
                QualifiedTypeMirror<Q> returnType,
                List<? extends QualifiedTypeMirror<Q>> thrownTypes,
                List<? extends QualifiedTypeVariable<Q>> typeVariables) {
            super(underlying, qualifier);
            checkUnderlyingKind(underlying, TypeKind.EXECUTABLE);
            checkTypeMirrorListsMatch("parameter",
                    parameterTypes, getUnderlyingType().getParameterTypes());
            checkTypeMirrorsMatch("receiver",
                    receiverType, getUnderlyingType().getReceiverType());
            checkTypeMirrorsMatch("return",
                    returnType, getUnderlyingType().getReturnType());
            checkTypeMirrorListsMatch("thrown",
                    thrownTypes, getUnderlyingType().getThrownTypes());
            checkTypeMirrorListsMatch("type variable",
                    typeVariables, getUnderlyingType().getTypeVariables());

            this.parameterTypes = new ArrayList<>(parameterTypes);
            this.receiverType = receiverType;
            this.returnType = returnType;
            this.thrownTypes = new ArrayList<>(thrownTypes);
            this.typeVariables = new ArrayList<>(typeVariables);
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitExecutable(this, p);
        }

        public ExecutableType getUnderlyingType() {
            return (ExecutableType)super.getUnderlyingType();
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

        public List<? extends QualifiedTypeVariable<Q>> getTypeVariables() {
            return typeVariables;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getQualifier()).append(" : ");

            if (typeVariables.size() > 0) {
                sb.append("<").append(commaSeparatedList(typeVariables)).append(">");
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
    }

    public static final class QualifiedIntersectionType<Q> extends QualifiedTypeMirror<Q> {
        private final List<? extends QualifiedTypeMirror<Q>> bounds;

        public QualifiedIntersectionType(TypeMirror underlying, Q qualifier,
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

        public IntersectionType getUnderlyingType() {
            return (IntersectionType)super.getUnderlyingType();
        }

        public List<? extends QualifiedTypeMirror<Q>> getBounds() {
            return bounds;
        }

        public String toString() {
            return punctuatedList(" & ", bounds);
        }
    }

    public static final class QualifiedNoType<Q> extends QualifiedTypeMirror<Q> {
        public QualifiedNoType(TypeMirror underlying, Q qualifier) {
            super(underlying, qualifier);
            // According to the NoType javadocs, valid kinds are NONE, PACKAGE,
            // and VOID.
            checkUnderlyingKindIsOneOf(underlying,
                    TypeKind.NONE, TypeKind.PACKAGE, TypeKind.VOID);
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitNoType(this, p);
        }

        public NoType getUnderlyingType() {
            return (NoType)super.getUnderlyingType();
        }
    }

    public static final class QualifiedNullType<Q> extends QualifiedTypeMirror<Q> {
        public QualifiedNullType(TypeMirror underlying, Q qualifier) {
            super(underlying, qualifier);
            checkUnderlyingKind(underlying, TypeKind.NULL);
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitNull(this, p);
        }

        public NullType getUnderlyingType() {
            return (NullType)super.getUnderlyingType();
        }
    }

    public static final class QualifiedPrimitiveType<Q> extends QualifiedTypeMirror<Q> {
        public QualifiedPrimitiveType(TypeMirror underlying, Q qualifier) {
            super(underlying, qualifier);
            checkUnderlyingKindIsPrimitive(underlying);
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitPrimitive(this, p);
        }

        public PrimitiveType getUnderlyingType() {
            return (PrimitiveType)super.getUnderlyingType();
        }
    }

    // There is no QualifiedReferenceType.  If we really need one, we can add
    // it to the hierarchy as an empty abstract class between
    // QualifiedTypeMirror and the qualified reference types (ArrayType,
    // DeclaredType, NullType, TypeVariable).

    public static final class QualifiedTypeVariable<Q> extends QualifiedTypeMirror<Q> {
        private final QualifiedTypeMirror<Q> upperBound;
        private final QualifiedTypeMirror<Q> lowerBound;

        public QualifiedTypeVariable(TypeMirror underlying, Q qualifier,
                QualifiedTypeMirror<Q> upperBound,
                QualifiedTypeMirror<Q> lowerBound) {
            super(underlying, qualifier);
            checkUnderlyingKind(underlying, TypeKind.TYPEVAR);
            checkTypeMirrorsMatch("upper bound",
                    upperBound, getUnderlyingType().getUpperBound());
            checkTypeMirrorsMatch("lower bound",
                    lowerBound, getUnderlyingType().getLowerBound());

            this.upperBound = upperBound;
            this.lowerBound = lowerBound;
        }

        @Override
        public <R,P> R accept(QualifiedTypeVisitor<Q,R,P> visitor, P p) {
            return visitor.visitTypeVariable(this, p);
        }

        public TypeVariable getUnderlyingType() {
            return (TypeVariable)super.getUnderlyingType();
        }

        public QualifiedTypeMirror<Q> getUpperBound() {
            return upperBound;
        }

        public QualifiedTypeMirror<Q> getLowerBound() {
            return lowerBound;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getQualifier()).append(" ")
                    .append(getUnderlyingType().asElement().getSimpleName())
                    .append(" extends ").append(upperBound)
                    .append(" super ").append(lowerBound);
            return sb.toString();
        }
    }

    public static final class QualifiedUnionType<Q> extends QualifiedTypeMirror<Q> {
        private final List<? extends QualifiedTypeMirror<Q>> alternatives;

        public QualifiedUnionType(TypeMirror underlying, Q qualifier,
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

        public UnionType getUnderlyingType() {
            return (UnionType)super.getUnderlyingType();
        }

        public List<? extends QualifiedTypeMirror<Q>> getAlternatives() {
            return alternatives;
        }

        public String toString() {
            return punctuatedList(" | ", alternatives);
        }
    }

    public static final class QualifiedWildcardType<Q> extends QualifiedTypeMirror<Q> {
        private final QualifiedTypeMirror<Q> extendsBound;
        private final QualifiedTypeMirror<Q> superBound;

        public QualifiedWildcardType(TypeMirror underlying, Q qualifier,
                QualifiedTypeMirror<Q> extendsBound,
                QualifiedTypeMirror<Q> superBound) {
            super(underlying, qualifier);
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

        public WildcardType getUnderlyingType() {
            return (WildcardType)super.getUnderlyingType();
        }

        public QualifiedTypeMirror<Q> getExtendsBound() {
            return extendsBound;
        }

        public QualifiedTypeMirror<Q> getSuperBound() {
            return superBound;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getQualifier()).append(" ?")
                    .append(" extends ").append(extendsBound)
                    .append(" super ").append(superBound);
            return sb.toString();
        }
    }
}


