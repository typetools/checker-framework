package org.checkerframework.framework.flow;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.StringsPlume;

import java.util.Objects;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;

/**
 * An implementation of an abstract value used by the Checker Framework
 * org.checkerframework.dataflow analysis.
 *
 * <p>A value holds a set of annotations and a type mirror. The set of annotations represents the
 * primary annotation on a type; therefore, the set of annotations must have an annotation for each
 * hierarchy unless the type mirror is a type variable or a wildcard that extends a type variable.
 * Both type variables and wildcards may be missing a primary annotation. For this set of
 * annotations, there is an additional constraint that only wildcards that extend type variables can
 * be missing annotations.
 *
 * <p>In order to compute {@link #leastUpperBound(CFAbstractValue)} and {@link
 * #mostSpecific(CFAbstractValue, CFAbstractValue)}, the case where one value has an annotation in a
 * hierarchy and the other does not must be handled. For type variables, the {@link
 * AnnotatedTypeVariable} for the declaration of the type variable is used. The {@link
 * AnnotatedTypeVariable} is computed using the type mirror. For wildcards, it is not always
 * possible to get the {@link AnnotatedWildcardType} for the type mirror. However, a
 * CFAbstractValue's type mirror is only a wildcard if the type of some expression is a wildcard.
 * The type of an expression is only a wildcard because the Checker Framework does not implement
 * capture conversion. For these uses of uncaptured wildcards, only the primary annotation on the
 * upper bound is ever used. So, the set of annotations represents the primary annotation on the
 * wildcard's upper bound. If that upper bound is a type variable, then the set of annotations could
 * be missing an annotation in a hierarchy.
 */
public abstract class CFAbstractValue<V extends CFAbstractValue<V>> implements AbstractValue<V> {

    /** The analysis class this value belongs to. */
    protected final CFAbstractAnalysis<V, ?, ?> analysis;

    /** The underlying (Java) type in this abstract value. */
    protected final TypeMirror underlyingType;
    /** The annotations in this abstract value. */
    protected final Set<AnnotationMirror> annotations;

    /**
     * Creates a new CFAbstractValue.
     *
     * @param analysis the analysis class this value belongs to
     * @param annotations the annotations in this abstract value
     * @param underlyingType the underlying (Java) type in this abstract value
     */
    protected CFAbstractValue(
            CFAbstractAnalysis<V, ?, ?> analysis,
            Set<AnnotationMirror> annotations,
            TypeMirror underlyingType) {
        this.analysis = analysis;
        this.annotations = annotations;
        this.underlyingType = underlyingType;

        assert validateSet(
                        this.getAnnotations(),
                        this.getUnderlyingType(),
                        analysis.getTypeFactory().getQualifierHierarchy())
                : "Encountered invalid type: "
                        + underlyingType
                        + " annotations: "
                        + StringsPlume.join(", ", annotations);
    }

    /**
     * Returns true if the set has an annotation from every hierarchy (or if it doesn't need to);
     * returns false if the set is missing an annotation from some hierarchy.
     *
     * @param annos set of annotations
     * @param typeMirror where the annotations are written
     * @param hierarchy the qualifier hierarchy
     * @return true if no annotations are missing
     */
    public static boolean validateSet(
            Set<AnnotationMirror> annos, TypeMirror typeMirror, QualifierHierarchy hierarchy) {

        if (canBeMissingAnnotations(typeMirror)) {
            return true;
        }

        Set<AnnotationMirror> missingHierarchy = null;
        for (AnnotationMirror top : hierarchy.getTopAnnotations()) {
            AnnotationMirror anno = hierarchy.findAnnotationInHierarchy(annos, top);
            if (anno == null) {
                if (missingHierarchy == null) {
                    missingHierarchy = AnnotationUtils.createAnnotationSet();
                }
                missingHierarchy.add(top);
            }
        }

        return missingHierarchy == null;
    }

    /**
     * Returns whether or not the set of annotations can be missing an annotation for any hierarchy.
     *
     * @return whether or not the set of annotations can be missing an annotation
     */
    public boolean canBeMissingAnnotations() {
        return canBeMissingAnnotations(underlyingType);
    }

    /**
     * Returns true if it is OK for the given type mirror not to be annotated, such as for VOID,
     * NONE, PACKAGE, TYPEVAR, and some WILDCARD.
     *
     * @param typeMirror a type
     * @return true if it is OK for the given type mirror not to be annotated
     */
    private static boolean canBeMissingAnnotations(TypeMirror typeMirror) {
        if (typeMirror == null) {
            return false;
        }
        if (typeMirror.getKind() == TypeKind.VOID
                || typeMirror.getKind() == TypeKind.NONE
                || typeMirror.getKind() == TypeKind.PACKAGE) {
            return true;
        }
        if (typeMirror.getKind() == TypeKind.WILDCARD) {
            return canBeMissingAnnotations(((WildcardType) typeMirror).getExtendsBound());
        }
        return typeMirror.getKind() == TypeKind.TYPEVAR;
    }

    /**
     * Returns a set of annotations. If {@link #canBeMissingAnnotations()} returns true, then the
     * set of annotations may not have an annotation for every hierarchy.
     *
     * <p>To get the single annotation in a particular hierarchy, use {@link
     * QualifierHierarchy#findAnnotationInHierarchy}.
     *
     * @return a set of annotations
     */
    @Pure
    public Set<AnnotationMirror> getAnnotations() {
        return annotations;
    }

    @Pure
    public TypeMirror getUnderlyingType() {
        return underlyingType;
    }

    @SuppressWarnings("interning:not.interned") // efficiency pre-test
    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof CFAbstractValue)) {
            return false;
        }

        CFAbstractValue<?> other = (CFAbstractValue<?>) obj;
        if (this.getUnderlyingType() != other.getUnderlyingType()
                && !analysis.getTypes()
                        .isSameType(this.getUnderlyingType(), other.getUnderlyingType())) {
            return false;
        }
        return AnnotationUtils.areSame(this.getAnnotations(), other.getAnnotations());
    }

    @Pure
    @Override
    public int hashCode() {
        return Objects.hash(getAnnotations(), underlyingType);
    }

    /**
     * Returns the string representation, using fully-qualified names.
     *
     * @return the string representation, using fully-qualified names
     */
    @SideEffectFree
    public String toStringFullyQualified() {
        return "CFAV{" + annotations + ", " + underlyingType + '}';
    }

    /**
     * Returns the string representation, using simple (not fully-qualified) names.
     *
     * @return the string representation, using simple (not fully-qualified) names
     */
    @SideEffectFree
    public String toStringSimple() {
        return "CFAV{"
                + AnnotationUtils.toStringSimple(annotations)
                + ", "
                + TypesUtils.simpleTypeName(underlyingType)
                + '}';
    }

    /**
     * Returns the string representation.
     *
     * @return the string representation
     */
    @SideEffectFree
    @Override
    public String toString() {
        return toStringSimple();
    }

    /**
     * Returns the more specific version of two values {@code this} and {@code other}. If they do
     * not contain information for all hierarchies, then it is possible that information from both
     * {@code this} and {@code other} are taken.
     *
     * <p>If neither of the two is more specific for one of the hierarchies (i.e., if the two are
     * incomparable as determined by {@link QualifierHierarchy#isSubtype(AnnotationMirror,
     * AnnotationMirror)}, then the respective value from {@code backup} is used.
     */
    public V mostSpecific(@Nullable V other, @Nullable V backup) {
        if (other == null) {
            @SuppressWarnings("unchecked")
            V v = (V) this;
            return v;
        }
        Types types = analysis.getTypes();
        TypeMirror mostSpecifTypeMirror;
        if (types.isAssignable(this.getUnderlyingType(), other.getUnderlyingType())) {
            mostSpecifTypeMirror = this.getUnderlyingType();
        } else if (types.isAssignable(other.getUnderlyingType(), this.getUnderlyingType())) {
            mostSpecifTypeMirror = other.getUnderlyingType();
        } else if (TypesUtils.isErasedSubtype(
                this.getUnderlyingType(), other.getUnderlyingType(), types)) {
            mostSpecifTypeMirror = this.getUnderlyingType();
        } else if (TypesUtils.isErasedSubtype(
                other.getUnderlyingType(), this.getUnderlyingType(), types)) {
            mostSpecifTypeMirror = other.getUnderlyingType();
        } else {
            mostSpecifTypeMirror = this.getUnderlyingType();
        }

        Set<AnnotationMirror> mostSpecific = AnnotationUtils.createAnnotationSet();
        MostSpecificVisitor ms =
                new MostSpecificVisitor(
                        mostSpecifTypeMirror,
                        this.getUnderlyingType(),
                        other.getUnderlyingType(),
                        this.getAnnotations(),
                        other.getAnnotations(),
                        backup,
                        mostSpecific);
        ms.visit();
        if (ms.error) {
            return backup;
        }
        return analysis.createAbstractValue(mostSpecific, mostSpecifTypeMirror);
    }

    /** Computes the most specific annotations. */
    private class MostSpecificVisitor extends AnnotationSetAndTypeMirrorVisitor {
        /** If set to true, then this visitor was unable to find a most specific annotation. */
        boolean error = false;

        /** Set of annotations to use if a most specific value cannot be found. */
        final Set<AnnotationMirror> backupSet;

        /** Set of most specific annotations. Annotations are added by the visitor. */
        final Set<AnnotationMirror> mostSpecific;

        /** TypeMirror for the "a" value. */
        final TypeMirror aTypeMirror;

        /** TypeMirror for the "b" value. */
        final TypeMirror bTypeMirror;

        /**
         * Create a {@link MostSpecificVisitor}.
         *
         * @param result the most specific type mirror
         * @param aTypeMirror type of the "a" value
         * @param bTypeMirror type of the "b" value
         * @param aSet annotations in the "a" value
         * @param bSet annotations in the "b" value
         * @param backup value to use if no most specific value is found
         * @param mostSpecific set to which the most specific value is added
         */
        public MostSpecificVisitor(
                TypeMirror result,
                TypeMirror aTypeMirror,
                TypeMirror bTypeMirror,
                Set<AnnotationMirror> aSet,
                Set<AnnotationMirror> bSet,
                V backup,
                Set<AnnotationMirror> mostSpecific) {
            super(result, aTypeMirror, bTypeMirror, aSet, bSet);
            this.aTypeMirror = aTypeMirror;
            this.bTypeMirror = bTypeMirror;
            this.mostSpecific = mostSpecific;
            if (backup != null) {
                this.backupSet = backup.getAnnotations();
                // this.backupTypeMirror = backup.getUnderlyingType();
                // this.backupAtv = getEffectTypeVar(backupTypeMirror);
            } else {
                // this.backupAtv = null;
                // this.backupTypeMirror = null;
                this.backupSet = null;
            }
        }

        private AnnotationMirror getBackUpAnnoIn(AnnotationMirror top) {
            if (backupSet == null) {
                // If there is no back up value, but one is required then the resulting set will
                // not be the most specific.  Indicate this with the error.
                error = true;
                return null;
            }
            QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
            return hierarchy.findAnnotationInHierarchy(backupSet, top);
        }

        @Override
        protected void visitAnnotationExistInBothSets(
                AnnotationMirror a, AnnotationMirror b, AnnotationMirror top) {
            QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
            if (analysis.getTypeFactory()
                            .hasQualifierParameterInHierarchy(
                                    TypesUtils.getTypeElement(aTypeMirror), top)
                    && analysis.getTypeFactory()
                            .hasQualifierParameterInHierarchy(
                                    TypesUtils.getTypeElement(bTypeMirror), top)) {
                // Both types have qualifier parameters, so the annotations must be exact.
                if (hierarchy.isSubtype(a, b) && hierarchy.isSubtype(b, a)) {
                    mostSpecific.add(b);
                } else {
                    AnnotationMirror backup = getBackUpAnnoIn(top);
                    if (backup != null) {
                        mostSpecific.add(backup);
                    }
                }
            } else {
                if (hierarchy.isSubtype(a, b)) {
                    mostSpecific.add(a);
                } else if (hierarchy.isSubtype(b, a)) {
                    mostSpecific.add(b);
                } else {
                    AnnotationMirror backup = getBackUpAnnoIn(top);
                    if (backup != null) {
                        mostSpecific.add(backup);
                    }
                }
            }
        }

        @Override
        protected void visitNeitherAnnotationExistsInBothSets(
                AnnotatedTypeVariable aAtv, AnnotatedTypeVariable bAtv, AnnotationMirror top) {
            if (canBeMissingAnnotations(result)) {
                // don't add an annotation
            } else {
                AnnotationMirror aUB = aAtv.getUpperBound().getEffectiveAnnotationInHierarchy(top);
                AnnotationMirror bUB = bAtv.getUpperBound().getEffectiveAnnotationInHierarchy(top);
                visitAnnotationExistInBothSets(aUB, bUB, top);
            }
        }

        @Override
        protected void visitAnnotationExistInOneSet(
                AnnotationMirror anno, AnnotatedTypeVariable atv, AnnotationMirror top) {
            QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
            AnnotationMirror upperBound = atv.getEffectiveAnnotationInHierarchy(top);

            if (!canBeMissingAnnotations(result)) {
                visitAnnotationExistInBothSets(anno, upperBound, top);
                return;
            }
            Set<AnnotationMirror> lBSet =
                    AnnotatedTypes.findEffectiveLowerBoundAnnotations(hierarchy, atv);
            AnnotationMirror lowerBound = hierarchy.findAnnotationInHierarchy(lBSet, top);
            if (hierarchy.isSubtype(upperBound, anno)) {
                // no anno is more specific than anno
            } else if (hierarchy.isSubtype(anno, lowerBound)) {
                mostSpecific.add(anno);
            } else {
                AnnotationMirror backup = getBackUpAnnoIn(top);
                if (backup != null) {
                    mostSpecific.add(backup);
                }
            }
        }
    }

    @Override
    public V leastUpperBound(@Nullable V other) {
        return upperBound(other, false);
    }

    public V widenUpperBound(@Nullable V other) {
        return upperBound(other, true);
    }

    private V upperBound(@Nullable V other, boolean shouldWiden) {
        if (other == null) {
            @SuppressWarnings("unchecked")
            V v = (V) this;
            return v;
        }
        ProcessingEnvironment processingEnv = analysis.getTypeFactory().getProcessingEnv();
        Set<AnnotationMirror> lub = AnnotationUtils.createAnnotationSet();
        TypeMirror lubTypeMirror =
                TypesUtils.leastUpperBound(
                        this.getUnderlyingType(), other.getUnderlyingType(), processingEnv);

        LubVisitor lubVisitor =
                new LubVisitor(
                        lubTypeMirror,
                        this.getUnderlyingType(),
                        other.getUnderlyingType(),
                        this.getAnnotations(),
                        other.getAnnotations(),
                        lub,
                        shouldWiden);
        lubVisitor.visit();
        return analysis.createAbstractValue(lub, lubTypeMirror);
    }

    class LubVisitor extends AnnotationSetAndTypeMirrorVisitor {
        Set<AnnotationMirror> lubSet;
        boolean widen;

        public LubVisitor(
                TypeMirror result,
                TypeMirror aTypeMirror,
                TypeMirror bTypeMirror,
                Set<AnnotationMirror> aSet,
                Set<AnnotationMirror> bSet,
                Set<AnnotationMirror> lubSet,
                boolean shouldWiden) {
            super(result, aTypeMirror, bTypeMirror, aSet, bSet);
            this.lubSet = lubSet;
            this.widen = shouldWiden;
        }

        private AnnotationMirror computeUpperBound(AnnotationMirror a, AnnotationMirror b) {
            QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
            if (widen) {
                return hierarchy.widenedUpperBound(a, b);
            } else {
                return hierarchy.leastUpperBound(a, b);
            }
        }

        @Override
        protected void visitAnnotationExistInBothSets(
                AnnotationMirror a, AnnotationMirror b, AnnotationMirror top) {
            lubSet.add(computeUpperBound(a, b));
        }

        @Override
        protected void visitNeitherAnnotationExistsInBothSets(
                AnnotatedTypeVariable aAtv, AnnotatedTypeVariable bAtv, AnnotationMirror top) {
            if (canBeMissingAnnotations(result)) {
                // don't add an annotation
            } else {
                AnnotationMirror aUB = aAtv.getUpperBound().getEffectiveAnnotationInHierarchy(top);
                AnnotationMirror bUB = bAtv.getUpperBound().getEffectiveAnnotationInHierarchy(top);
                lubSet.add(computeUpperBound(aUB, bUB));
            }
        }

        @Override
        protected void visitAnnotationExistInOneSet(
                AnnotationMirror anno, AnnotatedTypeVariable atv, AnnotationMirror top) {
            QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
            AnnotationMirror upperBound =
                    atv.getUpperBound().getEffectiveAnnotationInHierarchy(top);
            if (!canBeMissingAnnotations(result)) {
                lubSet.add(computeUpperBound(anno, upperBound));
            } else {
                Set<AnnotationMirror> lBSet =
                        AnnotatedTypes.findEffectiveLowerBoundAnnotations(hierarchy, atv);
                AnnotationMirror lowerBound = hierarchy.findAnnotationInHierarchy(lBSet, top);
                if (!hierarchy.isSubtype(anno, lowerBound)) {
                    lubSet.add(computeUpperBound(anno, upperBound));
                }
            }
        }
    }

    /**
     * Iterates through two sets of AnnotationMirrors by hierarchy and calls one of three methods
     * depending on whether an annotation exists for the hierarchy in each set. Also, passes a
     * {@link AnnotatedTypeVariable} if an annotation does not exist.
     */
    protected abstract class AnnotationSetAndTypeMirrorVisitor {
        TypeMirror result;

        private AnnotatedTypeVariable aAtv;
        private AnnotatedTypeVariable bAtv;
        private Set<AnnotationMirror> aSet;
        private Set<AnnotationMirror> bSet;

        protected AnnotationSetAndTypeMirrorVisitor(
                TypeMirror result,
                TypeMirror aTypeMirror,
                TypeMirror bTypeMirror,
                Set<AnnotationMirror> aSet,
                Set<AnnotationMirror> bSet) {
            this.result = result;
            this.aSet = aSet;
            this.bSet = bSet;
            this.aAtv = getEffectTypeVar(aTypeMirror);
            this.bAtv = getEffectTypeVar(bTypeMirror);
        }

        void visit() {
            QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
            Set<? extends AnnotationMirror> tops = hierarchy.getTopAnnotations();
            for (AnnotationMirror top : tops) {
                AnnotationMirror a = hierarchy.findAnnotationInHierarchy(aSet, top);
                AnnotationMirror b = hierarchy.findAnnotationInHierarchy(bSet, top);
                if (a != null && b != null) {
                    visitAnnotationExistInBothSets(a, b, top);
                } else if (a != null) {
                    visitAnnotationExistInOneSet(a, bAtv, top);
                } else if (b != null) {
                    visitAnnotationExistInOneSet(b, aAtv, top);
                } else {
                    visitNeitherAnnotationExistsInBothSets(aAtv, bAtv, top);
                }
            }
        }

        protected abstract void visitAnnotationExistInBothSets(
                AnnotationMirror a, AnnotationMirror b, AnnotationMirror top);

        protected abstract void visitNeitherAnnotationExistsInBothSets(
                AnnotatedTypeVariable aAtv, AnnotatedTypeVariable bAtv, AnnotationMirror top);

        protected abstract void visitAnnotationExistInOneSet(
                AnnotationMirror anno, AnnotatedTypeVariable atv, AnnotationMirror top);
    }

    /**
     * Returns the AnnotatedTypeVariable associated with the given TypeMirror or null.
     *
     * <p>If TypeMirror is a type variable, then the AnnotatedTypeVariable return is the declaration
     * of that TypeMirror. If the TypeMirror is a wildcard that extends a type variable, the
     * AnnotatedTypeVariable return is the declaration of that type variable. Otherwise, null is
     * returned.
     */
    private AnnotatedTypeVariable getEffectTypeVar(TypeMirror typeMirror) {
        if (typeMirror == null) {
            return null;
        } else if (typeMirror.getKind() == TypeKind.WILDCARD) {
            return getEffectTypeVar(((WildcardType) typeMirror).getExtendsBound());

        } else if (typeMirror.getKind() == TypeKind.TYPEVAR) {
            TypeVariable typevar = ((TypeVariable) typeMirror);
            AnnotatedTypeMirror atm =
                    analysis.getTypeFactory().getAnnotatedType(typevar.asElement());
            return (AnnotatedTypeVariable) atm;
        } else {
            return null;
        }
    }
}
