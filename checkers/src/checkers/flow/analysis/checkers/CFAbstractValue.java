package checkers.flow.analysis.checkers;

import javax.lang.model.element.AnnotationMirror;

import checkers.flow.analysis.AbstractValue;
import checkers.flow.util.HashCodeUtils;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.QualifierHierarchy;
import checkers.types.TypeHierarchy;
import checkers.util.AnnotatedTypes;

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

    protected final TypeHierarchy typeHierarchy;

    /**
     * The type (with annotations) corresponding to this abstract value.
     */
    protected final AnnotatedTypeMirror type;

    public CFAbstractValue(CFAbstractAnalysis<V, ?, ?> analysis,
            AnnotatedTypeMirror type) {
        this.analysis = analysis;
        this.type = type;
        this.typeHierarchy = analysis.getTypeHierarchy();
        assert type != null;
    }

    public AnnotatedTypeMirror getType() {
        return type;
    }

    /**
     * Computes and returns the least upper bound of two sets of type
     * annotations.
     */
    @Override
    public V leastUpperBound(/* @Nullable */V other) {
        if (other == null) {
            @SuppressWarnings("unchecked")
            V v = (V) this;
            return v;
        }
        analysis.getFactory().getQualifierHierarchy();
        AnnotatedTypeMirror lubType = AnnotatedTypes.asSuper(analysis
                .getFactory().getProcessingEnv().getTypeUtils(),
                analysis.getFactory(), this.getType(), other.getType());
        assert lubType != null : "Unable to compute LUB for " + getType().toString(true)
                + " and " + other.getType().toString(true) + ".";
        return analysis.createAbstractValue(lubType);
    }

    /**
     * Returns whether this value is a subtype of the argument {@code other}.
     * The annotations are compared per hierarchy, and missing annotations are
     * treated as 'top'.
     */
    public boolean isSubtypeOf(CFAbstractValue<V> other) {
        if (other == null) {
            // 'null' is 'top'
            return true;
        }
        return typeHierarchy.isSubtype(type, other.getType());
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
    // protected boolean isSubtype(int topIndex, InferredAnnotation a,
    // InferredAnnotation b) {
    // Set<AnnotationMirror> as;
    // Set<AnnotationMirror> bs;
    // AnnotationMirror top = tops[topIndex];
    // Set<AnnotationMirror> topSet = Collections.singleton(top);
    // if (b == null) {
    // // null is top
    // bs = topSet;
    // } else {
    // bs = b.getAnnotations();
    // }
    // if (a == null) {
    // // null is top
    // as = topSet;
    // } else {
    // as = a.getAnnotations();
    // }
    // if (bs.isEmpty()) {
    // // [] is a supertype of any qualifier, and [] <: []
    // return true;
    // }
    // if (as.isEmpty()) {
    // // [] is a subtype of no qualifier (only [])
    // return false;
    // }
    // assert as.size() == 1 && bs.size() == 1;
    // return analysis.qualifierHierarchy.isSubtype(as, bs);
    // }

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
        // Create new full type (with the same underlying type), and then add
        // the appropriate annotations.
        AnnotatedTypeMirror result = AnnotatedTypeMirror.createType(
                type.getUnderlyingType(), analysis.getFactory());
        QualifierHierarchy qualHierarchy = analysis.getFactory()
                .getQualifierHierarchy();
        AnnotatedTypeMirror otherType = other.getType();
        for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
            AnnotationMirror aAnno = getType().getAnnotationInHierarchy(top);
            AnnotationMirror bAnno = otherType.getAnnotationInHierarchy(top);

            if (qualHierarchy.isSubtype(aAnno, bAnno)) {
                result.addAnnotation(aAnno);
            } else if (qualHierarchy.isSubtype(bAnno, aAnno)) {
                result.addAnnotation(bAnno);
            } else {
                if (backup == null) {
                    assert false : "Neither of the two values is more specific: "
                            + this + ", " + other + ".";
                } else {
                    result.addAnnotation(backup.getType()
                            .getAnnotationInHierarchy(top));
                }
            }
        }
        return analysis.createAbstractValue(result);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CFAbstractValue)) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        boolean result = getType().equals(((CFAbstractValue) obj).getType());
        return result;
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash(type);
    }

    /**
     * @return The string representation as a comma-separated list.
     */
    @Override
    public String toString() {
        return getType().toString();
    }

    /**
     * Returns a string representation of an {@link AnnotationMirror}.
     */
    protected static String annotationToString(AnnotationMirror a) {
        String fullString = a.toString();
        int indexOfParen = fullString.indexOf("(");
        String annoName = fullString;
        if (indexOfParen >= 0) {
            annoName = fullString.substring(0, indexOfParen);
        }
        return fullString.substring(annoName.lastIndexOf('.') + 1,
                fullString.length());
    }
}