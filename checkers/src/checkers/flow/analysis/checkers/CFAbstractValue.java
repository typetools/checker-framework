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
            InferredAnnotation[] annotations) {
        this.analysis = analysis;
        this.tops = analysis.tops;
        this.annotations = annotations;
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
            Set<AnnotationMirror> thisAnnos;
            Set<AnnotationMirror> otherAnnos;
            AnnotationMirror top = tops[i];
            Set<AnnotationMirror> topSet = Collections.singleton(top);
            if (thisAnno == null) {
                // null is top
                thisAnnos = topSet;
            } else {
                thisAnnos = thisAnno.getAnnotations();
            }
            if (otherAnno == null) {
                // null is top
                otherAnnos = topSet;
            } else {
                otherAnnos = otherAnno.getAnnotations();
            }
            if (thisAnnos.isEmpty() || otherAnnos.isEmpty()) {
                // LUB must be [], as [] is the top of the hierarchy
                resultAnnotations[i] = NoInferredAnnotation.INSTANCE;
            } else {
                // Compute lub using the qualifier hierarchy.
                assert thisAnnos.size() == 1 && otherAnnos.size() == 1;
                Set<AnnotationMirror> lub = analysis.qualifierHierarchy
                        .leastUpperBounds(thisAnnos, otherAnnos);
                assert lub.size() == 1;
                resultAnnotations[i] = new InferredAnnotation(lub.iterator()
                        .next());
            }
        }

        return analysis.createAbstractValue(resultAnnotations);
    }

    /**
     * Returns whether this value is a subtype of the argument {@code other}.
     * The annotations are compared per hierarchy, and missing annotations are
     * treated as 'top'.
     */
    public boolean isSubtypeOf(CFAbstractValue<V> other) {
        boolean result = true;
        for (int i = 0; i < tops.length; i++) {
            InferredAnnotation thisAnno = annotations[i];
            InferredAnnotation otherAnno = other.annotations[i];
            result &= isSubtype(i, thisAnno, otherAnno);
        }
        return result;
    }

    /**
     * Returns whether {@code a} is a subtype of {@code b}, where it is assumed
     * that both {@code a} and {@code b} are part of the hierarchy identified by
     * {@code topIndex}.
     *
     * <p>
     * If one of the arguments is {@code null}, then it is assumed that 'top'
     * (from that hierarchy) is meant.
     *
     * <p>
     * Arguments can be {@link NoInferredAnnotation}, which can occur for
     * generics. In that case, subtyping is handled as follows, where we use
     * {@code []} to denote {@link NoInferredAnnotation}:
     * <ul>
     * <li>{@code [] <: []},
     * <li>{@code [] <: @A} for an annotation {@code A} if an only if {@code A}
     * is 'top' from this hierarchy,
     * <li>{@code false} otherwise.
     * </ul>
     */
    protected boolean isSubtype(int topIndex, InferredAnnotation a,
            InferredAnnotation b) {
        Set<AnnotationMirror> as;
        Set<AnnotationMirror> bs;
        AnnotationMirror top = tops[topIndex];
        Set<AnnotationMirror> topSet = Collections.singleton(top);
        if (b == null) {
            // null is top
            bs = topSet;
        } else {
            bs = b.getAnnotations();
        }
        if (a == null) {
            // null is top
            as = topSet;
        } else {
            as = a.getAnnotations();
        }
        if (bs.isEmpty()) {
            // [] is a supertype of any qualifier, and [] <: []
            return true;
        }
        if (as.isEmpty()) {
            // [] is a subtype of no qualifier (only [])
            return false;
        }
        assert as.size() == 1 && bs.size() == 1;
        return analysis.qualifierHierarchy.isSubtype(as, bs);
    }

    /**
     * Returns the more specific version of two values {@code this} and
     * {@code other}. If they do not contain information for all hierarchies,
     * then it is possible that information from both {@code this} and
     * {@code other} are taken.
     *
     * <p>
     * If neither of the two is more specific for one of the hierarchies (i.e.,
     * if the two are incomparable as determined by
     * {@link #isSubtype(int, InferredAnnotation, InferredAnnotation)}, then the
     * respective value from {@code backup} is used. If {@code backup} is
     * {@code null}, then an assertion error is raised.
     */
    public V mostSpecific(/* @Nullable */V other, /* @Nullable */V backup) {
        if (other == null) {
            @SuppressWarnings("unchecked")
            V v = (V) this;
            return v;
        }
        InferredAnnotation[] resultAnnotations = new InferredAnnotation[tops.length];
        for (int i = 0; i < tops.length; i++) {
            InferredAnnotation aAnno = annotations[i];
            InferredAnnotation bAnno = other.annotations[i];

            if (isSubtype(i, aAnno, bAnno)) {
                resultAnnotations[i] = aAnno;
            } else if (isSubtype(i, bAnno, aAnno)) {
                resultAnnotations[i] = bAnno;
            } else {
                if (backup == null) {
                    assert false : "Neither of the two values is more specific: "
                            + this + ", " + other + ".";
                } else {
                    resultAnnotations[i] = backup.annotations[i];
                }
            }
        }
        return analysis.createAbstractValue(resultAnnotations);
    }

    /**
     * Returns the array of {@link InferredAnnotation}s for a given annotation.
     * Only the annotation {@code a} is set in this array, all other hierarchies
     * are assume to not have any information.
     */
    public static InferredAnnotation[] createInferredAnnotationArray(
            CFAbstractAnalysis<?, ?, ?> analysis, AnnotationMirror a) {
        AnnotationMirror top = analysis.qualifierHierarchy.getTopAnnotation(a);
        InferredAnnotation[] annotations = new InferredAnnotation[analysis.tops.length];
        int index = CFAbstractValue.getIndex(top, analysis);
        annotations[index] = new InferredAnnotation(a);
        return annotations;
    }

    /**
     * Returns the {@link InferredAnnotation} in the hierarchy of {@code p}.
     * {@code p} does not need to be the root of a hierarchy.
     */
    public InferredAnnotation getAnnotationInHierarchy(AnnotationMirror p) {
        AnnotationMirror top = analysis.qualifierHierarchy.getTopAnnotation(p);
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
                result.add(annotationToString(tops[i]) + "->[]");
            } else {
                result.add(annotationToString(a.getAnnotation()));
            }
        }
        return result.toString();
    }

    /**
     * Returns a string representation of an {@link AnnotationMirror}.
     */
    protected String annotationToString(AnnotationMirror a) {
        String fullString = a.toString();
        int indexOfParen = fullString.indexOf("(");
        String annoName = fullString;
        if (indexOfParen >= 0) {
            annoName = fullString.substring(0, indexOfParen);
        }
        return fullString.substring(annoName.lastIndexOf('.') + 1,
                fullString.length());
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

        @Override
        public String toString() {
            return annotation.toString();
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

        @Override
        public String toString() {
            return "[]";
        }
    }
}