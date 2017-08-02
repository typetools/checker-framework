package org.checkerframework.framework.flow;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.util.HashCodeUtils;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TypesUtils;

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
 *
 * @author Stefan Heule
 */
public abstract class CFAbstractValue<V extends CFAbstractValue<V>> implements AbstractValue<V> {

    /** The analysis class this value belongs to. */
    protected final CFAbstractAnalysis<V, ?, ?> analysis;

    protected final TypeMirror underlyingType;
    protected final Set<AnnotationMirror> annotations;

    public CFAbstractValue(
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
                        + PluginUtil.join(", ", annotations);
    }

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
     * Returns whether or not the set of annotations can be missing an annotation for any hierarchy
     *
     * @return whether or not the set of annotations can be missing an annotation
     */
    public boolean canBeMissingAnnotations() {
        return canBeMissingAnnotations(underlyingType);
    }

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
     * @return returns a set of annotations
     */
    @Pure
    public Set<AnnotationMirror> getAnnotations() {
        return annotations;
    }

    @Pure
    public TypeMirror getUnderlyingType() {
        return underlyingType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CFAbstractValue)) {
            return false;
        }

        CFAbstractValue<?> other = (CFAbstractValue<?>) obj;
        if (!analysis.getTypes().isSameType(this.getUnderlyingType(), other.getUnderlyingType())) {
            return false;
        }
        return AnnotationUtils.areSame(this.getAnnotations(), other.getAnnotations());
    }

    @Pure
    @Override
    public int hashCode() {
        Collection<Object> objects = new ArrayList<>();
        objects.addAll(getAnnotations());
        objects.add(underlyingType);
        return HashCodeUtils.hash(objects);
    }

    /** @return the string representation as a comma-separated list */
    @SideEffectFree
    @Override
    public String toString() {
        return "CFAbstractValue{"
                + "annotations="
                + annotations
                + ", underlyingType="
                + underlyingType
                + '}';
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
    public V mostSpecific(/*@Nullable*/ V other, /*@Nullable*/ V backup) {
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
                types, this.getUnderlyingType(), other.getUnderlyingType())) {
            mostSpecifTypeMirror = this.getUnderlyingType();
        } else if (TypesUtils.isErasedSubtype(
                types, other.getUnderlyingType(), this.getUnderlyingType())) {
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

    private class MostSpecificVisitor extends AnnotationSetAndTypeMirrorVisitor {
        boolean error = false;
        // TypeMirror backupTypeMirror;
        Set<AnnotationMirror> backupSet;
        // AnnotatedTypeVariable backupAtv;
        Set<AnnotationMirror> mostSpecific;

        public MostSpecificVisitor(
                TypeMirror result,
                TypeMirror aTypeMirror,
                TypeMirror bTypeMirror,
                Set<AnnotationMirror> aSet,
                Set<AnnotationMirror> bSet,
                V backup,
                Set<AnnotationMirror> mostSpecific) {
            super(result, aTypeMirror, bTypeMirror, aSet, bSet);
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
    public V leastUpperBound(/*@Nullable*/ V other) {
        return upperBound(other, false);
    }

    public V widenUpperBound(/*@Nullable*/ V other) {
        return upperBound(other, true);
    }

    private V upperBound(/*@Nullable*/ V other, boolean shouldWiden) {
        if (other == null) {
            @SuppressWarnings("unchecked")
            V v = (V) this;
            return v;
        }
        ProcessingEnvironment processingEnv = analysis.getTypeFactory().getProcessingEnv();
        Set<AnnotationMirror> lub = AnnotationUtils.createAnnotationSet();
        TypeMirror lubTypeMirror =
                InternalUtils.leastUpperBound(
                        processingEnv, this.getUnderlyingType(), other.getUnderlyingType());

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

        public AnnotationSetAndTypeMirrorVisitor(
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
