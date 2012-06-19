package checkers.flow.analysis.checkers;

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

    /** The annotation corresponding to this abstract value. */
    protected Set<AnnotationMirror> annotations;

    public CFAbstractValue(CFAbstractAnalysis<V, ?, ?> analysis,
            Set<AnnotationMirror> annotations) {
        this.analysis = analysis;
        assert areValidAnnotations(annotations);
        this.annotations = annotations;
    }

    /**
     * Are the annotations {@code annotations} valid for the given analysis?
     */
    protected boolean areValidAnnotations(Set<AnnotationMirror> annotations) {
        for (AnnotationMirror a : annotations) {
            if (!AnnotationUtils.containsSameIgnoringValues(analysis.supportedAnnotations, a)) {
                return false;
            }
        }
        return true;
    }

    /** @return The annotations this abstract value stands for. */
    public Set<AnnotationMirror> getAnnotations() {
        return annotations;
    }

    /**
     * Computes and returns the least upper bound of two sets of type
     * annotations. The return value is always of type
     * DefaultTypeAnalysis.Value.
     */
    @Override
    public/* @Nullable */V leastUpperBound(V other) {
        Set<AnnotationMirror> lub = analysis.qualifierHierarchy
                .leastUpperBound(annotations, other.annotations);
        return analysis.createAbstractValue(lub);
    }

    /**
     * Return whether this Value is a proper subtype of the argument Value.
     */
    public boolean isSubtypeOf(CFAbstractValue<V> other) {
        return analysis.qualifierHierarchy.isSubtype(annotations,
                other.annotations);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CFAbstractValue)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        CFAbstractValue<V> other = (CFAbstractValue<V>) obj;
        return AnnotationUtils
                .areSame(getAnnotations(), other.getAnnotations());
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash(annotations);
    }

    /**
     * @return The string representation as a comma-separated list.
     */
    @Override
    public String toString() {
        return annotations.toString();
    }
}