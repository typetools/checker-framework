package checkers.flow.analysis.checkers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import checkers.flow.analysis.AbstractValue;
import checkers.flow.util.HashCodeUtils;
import checkers.util.AnnotationUtils;

/**
 * An implementation of an abstract value used by the Checker Framework dataflow
 * analysis. Contains a set of annotations.
 *
 * @author Stefan Heule
 *
 */
public abstract class CFAbstractValue<V extends CFAbstractValue<V>> implements
        AbstractValue<V> {

    /**
     * The analysis class this store belongs to.
     */
    protected final CFAbstractAnalysis<V, ?, ?> analysis;

    /** The 'top' annotations in all hierarchies. */
    protected final AnnotationMirror[] tops;

    /**
     * The annotations corresponding to this abstract value. They are stored in
     * a mapping from the root of every hierarchy to a
     * {@link InferredAnnotation}. Because the mapping usually only contains one
     * or two entries, an array is used. The item at position {@code i}
     * corresponds to the hierarchy with root annotation {@code tops[i]}.
     *
     * <p>
     * Every position of the index can be one of three possibilities:
     * <ul>
     * <li>{@code null}. This indicates that no information about this hierarchy
     * has been inferred.
     * <li>An object of type {@link NoInferredAnnotation}. This indicates that
     * "no annotation" has been inferred, which is different from
     * "no information available". This is used for generics.
     * <li>An object of type {@link InferredAnnotation}. In this case, an
     * annotation has been inferred.
     * </ul>
     */
    protected final InferredAnnotation[] annotations;

    public CFAbstractValue(CFAbstractAnalysis<V, ?, ?> analysis,
            Set<AnnotationMirror> annotationSet) {
        this.analysis = analysis;
        assert areValidAnnotations(annotationSet);
        tops = analysis.tops;
        annotations = new InferredAnnotation[tops.length];
        for (int i = 0; i < tops.length; i++) {
            annotations[i] = NoInferredAnnotation.INSTANCE;
        }
        for (AnnotationMirror anno : annotationSet) {
            AnnotationMirror top = analysis.qualifierHierarchy
                    .getRootAnnotation(anno);
            annotations[getIndex(top)] = new InferredAnnotation(anno);
        }
    }

    public CFAbstractValue(CFAbstractAnalysis<V, ?, ?> analysis,
            InferredAnnotation[] annotations) {
        this.analysis = analysis;
        this.tops = analysis.tops;
        this.annotations = annotations;
    }

    /**
     * Are the annotations {@code annotations} valid for the given analysis?
     */
    protected boolean areValidAnnotations(Set<AnnotationMirror> annotations) {
        for (AnnotationMirror a : annotations) {
            if (!AnnotationUtils.containsSameIgnoringValues(
                    analysis.supportedAnnotations, a)) {
                return false;
            }
        }
        return true;
    }

    /** Returns the annotation of the hierarchy identified by 'top'. */
    public InferredAnnotation getAnnotation(AnnotationMirror top) {
        return annotations[getIndex(top)];
    }

    /** Returns the index of a hierarchy identified by {@code top}. */
    protected int getIndex(AnnotationMirror top) {
        for (int i = 0; i < tops.length; i++) {
            if (AnnotationUtils.areSame(top, tops[i])) {
                return i;
            }
        }
        assert false;
        return -1;
    }

    /** Returns the index of a hierarchy identified by {@code top}. */
    public static int getIndex(AnnotationMirror top,
            CFAbstractAnalysis<?, ?, ?> analysis) {
        for (int i = 0; i < analysis.tops.length; i++) {
            if (AnnotationUtils.areSame(top, analysis.tops[i])) {
                return i;
            }
        }
        assert false;
        return -1;
    }

    /**
     * Computes and returns the least upper bound of two sets of type
     * annotations. The return value is always of type
     * DefaultTypeAnalysis.Value.
     */
    @Override
    public/* @Nullable */V leastUpperBound(V other) {
        if (other == null) {
            @SuppressWarnings("unchecked")
            V v = (V) this;
            return v;
        }
        InferredAnnotation[] resultAnnotations = new InferredAnnotation[tops.length];
        for (int i = 0; i < tops.length; i++) {
            InferredAnnotation thisAnno = annotations[i];
            InferredAnnotation otherAnno = other.annotations[i];
            if (thisAnno == null) { // thisAnno is top
                resultAnnotations[i] = otherAnno;
            } else if (otherAnno == null) { // otherAnno is top
                resultAnnotations[i] = thisAnno;
            } else {
                // Compute lub using the qualifier hierarchy.
                Set<AnnotationMirror> lub = analysis.qualifierHierarchy
                        .leastUpperBound(thisAnno.getAnnotations(),
                                otherAnno.getAnnotations());
                if (lub.size() == 0) {
                    resultAnnotations[i] = NoInferredAnnotation.INSTANCE;
                } else {
                    assert lub.size() == 1;
                    resultAnnotations[i] = new InferredAnnotation(lub
                            .iterator().next());
                }
            }
        }

        return analysis.createAbstractValue(resultAnnotations);
    }

    /**
     * Returns whether this value is a proper subtype of the argument
     * {@code other}. The annotations are compared per hierarchy, and missing
     * annotations are treated as 'top'.
     */
    public boolean isSubtypeOf(CFAbstractValue<V> other) {
        for (int i = 0; i < tops.length; i++) {
            InferredAnnotation thisAnno = annotations[i];
            InferredAnnotation otherAnno = other.annotations[i];
            Set<AnnotationMirror> thisAnnos;
            Set<AnnotationMirror> otherAnnos;
            if (otherAnno == null) { // thisAnno is top
                otherAnnos = Collections.singleton(tops[i]);
            } else {
                otherAnnos = otherAnno.getAnnotations();
            }
            if (thisAnno == null) { // otherAnno is top
                thisAnnos = Collections.singleton(tops[i]);
            } else {
                thisAnnos = thisAnno.getAnnotations();
            }
            if (!analysis.qualifierHierarchy.isSubtype(thisAnnos, otherAnnos)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the more specific version of two values {@code this} and {@code other}.
     * If they do not contain information for all hierarchies, then it is
     * possible that information from both {@code this} and {@code other} are taken.
     */
    public V mostSpecific(/* @Nullable */V other) {
        if (other == null) {
            @SuppressWarnings("unchecked")
            V v = (V) this;
            return v;
        }
        InferredAnnotation[] resultAnnotations = new InferredAnnotation[tops.length];
        for (int i = 0; i < tops.length; i++) {
            InferredAnnotation aAnno = annotations[i];
            InferredAnnotation bAnno = other.annotations[i];
            if (aAnno == null) {
                resultAnnotations[i] = bAnno;
            } else if (bAnno == null) {
                resultAnnotations[i] = aAnno;
            } else {
                // Compute the more specific annotation using the qualifier
                // hierarchy.
                boolean subtype = analysis.qualifierHierarchy.isSubtype(
                        aAnno.getAnnotations(), bAnno.getAnnotations());
                if (subtype) {
                    resultAnnotations[i] = aAnno;
                } else {
                    resultAnnotations[i] = bAnno;
                }
            }
        }
        return analysis.createAbstractValue(resultAnnotations);
    }

    /**
     * Returns the {@link InferredAnnotation} in the hierarchy of {@code p}.
     * {@code p} does not need to be the root of a hierarchy.
     */
    public InferredAnnotation getAnnotationInHierarchy(AnnotationMirror p) {
        AnnotationMirror top = analysis.qualifierHierarchy.getRootAnnotation(p);
        return getAnnotation(top);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CFAbstractValue)) {
            return false;
        }
        // For every hierarchy, the annotations must be the same.
        CFAbstractValue<?> other = (CFAbstractValue<?>) obj;
        for (int i = 0; i < tops.length; i++) {
            InferredAnnotation thisAnno = annotations[i];
            InferredAnnotation otherAnno = other.annotations[i];
            Set<AnnotationMirror> thisAnnos;
            Set<AnnotationMirror> otherAnnos;
            if (otherAnno == null) { // thisAnno is top
                otherAnnos = Collections.singleton(tops[i]);
            } else {
                otherAnnos = otherAnno.getAnnotations();
            }
            if (thisAnno == null) { // otherAnno is top
                thisAnnos = Collections.singleton(tops[i]);
            } else {
                thisAnnos = thisAnno.getAnnotations();
            }
            if (!AnnotationUtils.areSame(thisAnnos, otherAnnos)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash((Object[]) annotations);
    }

    /**
     * @return The string representation as a comma-separated list.
     */
    @Override
    public String toString() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < tops.length; i++) {
            InferredAnnotation a = annotations[i];
            if (a == null) {
                continue;
            } else if (a.isNoInferredAnnotation()) {
                result.add(tops[i].toString()+"->[]");
            } else {
                result.add(a.getAnnotation().toString());
            }
        }
        return result.toString();
    }

    /**
     * Used to represent the inferred annotation for one hierarchy. Can also be
     * {@link NoInferredAnnotation}, as flow can infer "no annotation".
     */
    public static class InferredAnnotation {
        /** The annotation. */
        protected final AnnotationMirror annotation;

        public InferredAnnotation(AnnotationMirror annotation) {
            this.annotation = annotation;
        }

        /** Is this {@link NoInferredAnnotation}? */
        public boolean isNoInferredAnnotation() {
            return false;
        }

        /**
         * Returns the annotation inferred (only be valid to be called if
         * {@code isNoInferredAnnotation()} is true.
         */
        public AnnotationMirror getAnnotation() {
            return annotation;
        }

        /**
         * Returns the annotation inferred as a set. Is either empty or contains
         * exactly one element.
         */
        public Set<AnnotationMirror> getAnnotations() {
            return Collections.singleton(annotation);
        }
    }

    /**
     * Represents the fact that no annotation has been inferred (for generic
     * types).
     */
    public static class NoInferredAnnotation extends InferredAnnotation {

        /** An object of type {@link NoInferredAnnotation} that can be used. */
        public final static NoInferredAnnotation INSTANCE = new NoInferredAnnotation();

        private NoInferredAnnotation() {
            super(null);
        }

        @Override
        public boolean isNoInferredAnnotation() {
            return true;
        }

        @Override
        public AnnotationMirror getAnnotation() {
            assert false;
            return null;
        }

        @Override
        public Set<AnnotationMirror> getAnnotations() {
            return Collections.emptySet();
        }
    }
}