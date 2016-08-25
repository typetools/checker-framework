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
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * An implementation of an abstract value used by the Checker Framework
 * org.checkerframework.dataflow analysis.
 *
 * @author Stefan Heule
 */
public abstract class CFAbstractValue<V extends CFAbstractValue<V>> implements AbstractValue<V> {

    /**
     * The analysis class this value belongs to.
     */
    protected final CFAbstractAnalysis<V, ?, ?> analysis;

    protected final TypeHierarchy typeHierarchy;
    protected final GenericAnnotatedTypeFactory<V, ?, ?, ?> factory;
    protected final ProcessingEnvironment processingEnv;
    protected final QualifierHierarchy hierarchy;
    private final TypeMirror underlyingType;
    private final Set<AnnotationMirror> annotations;

    public CFAbstractValue(
            CFAbstractAnalysis<V, ?, ?> analysis,
            Set<AnnotationMirror> annotations,
            TypeMirror underlyingType) {
        this.analysis = analysis;
        this.typeHierarchy = analysis.getTypeHierarchy();
        this.annotations = annotations;
        this.underlyingType = underlyingType;
        this.factory = analysis.getTypeFactory();
        this.processingEnv = factory.getProcessingEnv();
        this.hierarchy = analysis.getTypeFactory().getQualifierHierarchy();

        validateSet(this.getAnnotations(), this.getUnderlyingType());
    }

    private boolean validateSet(Set<AnnotationMirror> annos, TypeMirror typeMirror) {
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

        if (missingHierarchy != null) {
            String missing = PluginUtil.join(", ", missingHierarchy);
            ErrorReporter.errorAbort(
                    "CFAbstractValue: missing annotation in the following hierarchies: " + missing);
            return false;
        }
        return true;
    }

    /**
     * Returns whether or not the set of annotations can be missing an annotation for any hierarchy
     * @return whether or not the set of annotations can be missing an annotation
     */
    public boolean canBeMissingAnnotations() {
        return canBeMissingAnnotations(underlyingType);
    }

    private boolean canBeMissingAnnotations(TypeMirror typeMirror) {
        if (typeMirror == null) {
            return false;
        }
        if (typeMirror.getKind() == TypeKind.WILDCARD) {
            return canBeMissingAnnotations(((WildcardType) typeMirror).getExtendsBound());
        }
        return typeMirror.getKind() == TypeKind.TYPEVAR;
    }

    /**
     * Returns a set of annotations.  If {@link #canBeMissingAnnotations()} returns true, then
     * the set of annotations may not have an annotation for every hierarchy.
     * @return Returns a set of annotations
     */
    @Pure // because the annotations field is final
    public Set<AnnotationMirror> getAnnotations() {
        return annotations;
    }

    @Pure // because the underlyingType field is final
    public TypeMirror getUnderlyingType() {
        return underlyingType;
    }

    /**
     * Returns whether this value is a subtype of the argument {@code other}. The annotations are
     * compared per hierarchy, and missing annotations are treated as 'top'.
     */
    @Deprecated
    public boolean isSubtypeOf(CFAbstractValue<V> other) {
        //        if (other == null) {
        // 'null' is 'top'
        return true;
        //        }
        //        return typeHierarchy.isSubtype(type, other.getType());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CFAbstractValue)) {
            return false;
        }

        CFAbstractValue<?> other = (CFAbstractValue) obj;
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

    /**
     * @return the string representation as a comma-separated list
     */
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
     * <p> If neither of the two is more specific for one of the hierarchies (i.e., if the two are
     * incomparable as determined by {@link QualifierHierarchy#isSubtype(AnnotationMirror, AnnotationMirror)},
     * then the respective value from {@code backup} is used.
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
        TypeMirror backupTypeMirror;
        Set<AnnotationMirror> backupSet;
        AnnotatedTypeVariable backupAtv;
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
                this.backupTypeMirror = backup.getUnderlyingType();
                this.backupAtv = getEffectTypeVar(backupTypeMirror);
            } else {
                this.backupAtv = null;
                this.backupTypeMirror = null;
                this.backupSet = null;
            }
        }

        private AnnotationMirror getBackUpAnnoIn(AnnotationMirror top) {
            if (backupSet == null) {
                // If there is no back up value, but on is required then the resulting set will
                // not be the most specific.  Indicate this with the error.
                error = true;
                return null;
            }
            return hierarchy.findAnnotationInHierarchy(backupSet, top);
        }

        @Override
        protected void visitAnnotationExistInBothSets(
                AnnotationMirror a, AnnotationMirror b, AnnotationMirror top) {
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
        if (other == null) {
            @SuppressWarnings("unchecked")
            V v = (V) this;
            return v;
        }

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
                        lub);
        lubVisitor.visit();
        return analysis.createAbstractValue(lub, lubTypeMirror);
    }

    class LubVisitor extends AnnotationSetAndTypeMirrorVisitor {

        Set<AnnotationMirror> lubSet;

        public LubVisitor(
                TypeMirror result,
                TypeMirror aTypeMirror,
                TypeMirror bTypeMirror,
                Set<AnnotationMirror> aSet,
                Set<AnnotationMirror> bSet,
                Set<AnnotationMirror> lubSet) {
            super(result, aTypeMirror, bTypeMirror, aSet, bSet);
            this.lubSet = lubSet;
        }

        @Override
        protected void visitAnnotationExistInBothSets(
                AnnotationMirror a, AnnotationMirror b, AnnotationMirror top) {
            lubSet.add(hierarchy.leastUpperBound(a, b));
        }

        @Override
        protected void visitNeitherAnnotationExistsInBothSets(
                AnnotatedTypeVariable aAtv, AnnotatedTypeVariable bAtv, AnnotationMirror top) {
            if (canBeMissingAnnotations(result)) {
                // don't add an annotation
            } else {
                AnnotationMirror aUB = aAtv.getUpperBound().getEffectiveAnnotationInHierarchy(top);
                AnnotationMirror bUB = bAtv.getUpperBound().getEffectiveAnnotationInHierarchy(top);
                lubSet.add(hierarchy.leastUpperBound(aUB, bUB));
            }
        }

        @Override
        protected void visitAnnotationExistInOneSet(
                AnnotationMirror anno, AnnotatedTypeVariable atv, AnnotationMirror top) {
            AnnotationMirror upperBound =
                    atv.getUpperBound().getEffectiveAnnotationInHierarchy(top);
            if (!canBeMissingAnnotations(result)) {
                lubSet.add(hierarchy.leastUpperBound(anno, upperBound));
            } else {
                Set<AnnotationMirror> lBSet =
                        AnnotatedTypes.findEffectiveLowerBoundAnnotations(hierarchy, atv);
                AnnotationMirror lowerBound = hierarchy.findAnnotationInHierarchy(lBSet, top);
                if (!hierarchy.isSubtype(anno, lowerBound)) {
                    lubSet.add(hierarchy.leastUpperBound(anno, upperBound));
                }
            }
        }
    }

    /**
     * Iterates through two sets of AnnotationMirrors by hierarchy and calls one of three methods
     * depending on whether an annotation exists for the hierarchy in each set.  Also, passes a
     * {@link AnnotatedTypeVariable} if an annotation does not exist.
     */
    abstract class AnnotationSetAndTypeMirrorVisitor {
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
