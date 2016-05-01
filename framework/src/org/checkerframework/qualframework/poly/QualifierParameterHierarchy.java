package org.checkerframework.qualframework.poly;

import java.util.*;

import org.checkerframework.javacutil.Pair;

import org.checkerframework.qualframework.base.QualifierHierarchy;

/** This class provides a <code>QualifierHierarchy</code> implementation for
 * sets of qualifier parameters.  Under this hierarchy, A is a subtype of B iff
 * the value of each qualifier parameter in A is contained within the value of
 * the corresponding parameter in B.
 */
public class QualifierParameterHierarchy<Q> implements QualifierHierarchy<QualParams<Q>> {
    private QualifierHierarchy<PolyQual<Q>> polyQualHierarchy;
    private QualifierHierarchy<Wildcard<Q>> containmentHierarchy;
    private List<Pair<Wildcard<Q>, Wildcard<Q>>> constraintTarget = null;

    // The bottom QualParams in the QualifierHierarchy
    public QualParams<Q> PARAMS_BOTTOM;
    // The top QualParams in the QualifierHierarchy
    public QualParams<Q> PARAMS_TOP;

    public QualifierParameterHierarchy(QualifierHierarchy<Wildcard<Q>> containmentHierarchy,
            QualifierHierarchy<PolyQual<Q>> polyQualHierarchy) {
        this.containmentHierarchy = containmentHierarchy;
        this.polyQualHierarchy = polyQualHierarchy;

        setTop(polyQualHierarchy);
        setBottom(polyQualHierarchy);
    }

    // We can't use constructor overloads for the following variants because
    // they all have the same erasure.

    /** Construct an instance from a {@link ContainmentHierarchy} or
     * equivalent.
     */
    public static <Q> QualifierParameterHierarchy<Q> fromContainment(QualifierHierarchy<Wildcard<Q>> containmentHierarchy,
            QualifierHierarchy<PolyQual<Q>> polyQualHierarchy) {
        return new QualifierParameterHierarchy<>(containmentHierarchy, polyQualHierarchy);
    }

    /** Construct an instance from a {@link PolyQualHierarchy} or equivalent,
     * using the default {@link ContainmentHierarchy} implementation.
     */
    public static <Q> QualifierParameterHierarchy<Q> fromPolyQual(QualifierHierarchy<PolyQual<Q>> polyQualHierarchy) {
        return fromContainment(new ContainmentHierarchy<Q>(polyQualHierarchy), polyQualHierarchy);
    }

    /** Construct an instance from a {@code QualifierHierarchy<Q>}, using the
     * default {@link PolyQualHierarchy} and {@link ContainmentHierarchy}
     * implementations.
     */
    public static <Q> QualifierParameterHierarchy<Q> fromGround(QualifierHierarchy<Q> groundHierarchy) {
        return fromPolyQual(new PolyQualHierarchy<Q>(groundHierarchy));
    }


    /** Get the containment hierarchy that is used to compare wildcards. */
    protected QualifierHierarchy<Wildcard<Q>> getContaintmentHierarchy() {
        return containmentHierarchy;
    }

    /** Set a target for constraint generation.  When the current constraint
     * target is not {@code null}, all subtyping checks return {@code true} and
     * populate the target with a (subtype, supertype) pair for each
     * containment check that would normally be made.
     */
    public void setConstraintTarget(List<Pair<Wildcard<Q>, Wildcard<Q>>> constraintTarget) {
        this.constraintTarget = constraintTarget;
    }


    @Override
    public boolean isSubtype(QualParams<Q> subtype, QualParams<Q> supertype) {
        if (subtype.equals(supertype)) {
            return true;
        }

        if (subtype == PARAMS_BOTTOM || supertype == PARAMS_TOP) {
            return true;
        }

        // There is no corollary for PARAMS_BOTTOM, since the other would have to have every parameter.
        if (subtype == PARAMS_TOP
                && polyQualHierarchy.isSubtype(subtype.getPrimary(), supertype.getPrimary())
                && supertype.isEmpty()) {
            return true;
        }

        if (subtype == PARAMS_TOP || supertype == PARAMS_BOTTOM ||
                !subtype.keySet().equals(supertype.keySet())) {
            if (constraintTarget == null) {
                return false;
            } else {
                constraintTarget.add(null);
                return true;
            }
        }

        if (subtype.getPrimary() != null && supertype.getPrimary() != null) {
            if (constraintTarget == null) {
                if (!polyQualHierarchy.isSubtype(subtype.getPrimary(), supertype.getPrimary())) {
                    return false;
                }
            } else {
                constraintTarget.add(Pair.of(new Wildcard<>(subtype.getPrimary()), new Wildcard<>(supertype.getPrimary())));
            }
        }

        for (String k : subtype.keySet()) {
            if (constraintTarget == null) {
                if (!containmentHierarchy.isSubtype(subtype.get(k), supertype.get(k))) {
                    return false;
                }
            } else {
                constraintTarget.add(Pair.of(subtype.get(k), supertype.get(k)));
            }
        }

        return true;
    }

    @Override
    public QualParams<Q> leastUpperBound(QualParams<Q> a, QualParams<Q> b) {
        if (this.constraintTarget != null) {
            throw new UnsupportedOperationException("unexpected leastUpperBound when generating constraints");
        }

        if (a == PARAMS_BOTTOM) {
            return b;
        }

        if (b == PARAMS_BOTTOM) {
            return a;
        }

        if (a == PARAMS_TOP || b == PARAMS_TOP) {
            return PARAMS_TOP;
        }

        Map<String, Wildcard<Q>> result = new HashMap<>();

        for (String k : a.keySet()) {
            if (b.containsKey(k)) {
                result.put(k, containmentHierarchy.leastUpperBound(a.get(k), b.get(k)));
            } else {
                result.put(k, a.get(k));
            }
        }

        for (String k : b.keySet()) {
            if (!a.containsKey(k)) {
                result.put(k, b.get(k));
            }
        }

        PolyQual<Q> newPrimary = null;
        if (a.getPrimary() != null && b.getPrimary() != null) {
            newPrimary = polyQualHierarchy.leastUpperBound(a.getPrimary(), b.getPrimary());
        }
        return new QualParams<Q>(result, newPrimary);
    }

    @Override
    public QualParams<Q> greatestLowerBound(QualParams<Q> a, QualParams<Q> b) {
        if (this.constraintTarget != null) {
            throw new UnsupportedOperationException("unexpected leastUpperBound when generating constraints");
        }

        if (a == PARAMS_TOP) {
            return b;
        }

        if (b == PARAMS_TOP) {
            return a;
        }

        if (a == PARAMS_BOTTOM || b == PARAMS_BOTTOM) {
            return PARAMS_BOTTOM;
        }

        Map<String, Wildcard<Q>> result = new HashMap<>();

        for (String k : a.keySet()) {
            if (b.containsKey(k)) {
                result.put(k, containmentHierarchy.greatestLowerBound(a.get(k), b.get(k)));
            } else {
                result.put(k, a.get(k));
            }
        }

        for (String k : b.keySet()) {
            if (!a.containsKey(k)) {
                result.put(k, b.get(k));
            }
        }

        PolyQual<Q> newPrimary = null;
        if (a.getPrimary() != null && b.getPrimary() != null) {
            newPrimary = polyQualHierarchy.greatestLowerBound(a.getPrimary(), b.getPrimary());
        }
        return new QualParams<Q>(result, newPrimary);
    }

    /*package*/ static final String PARAMS_BOTTOM_TO_STRING = "__@RegexBottom__";
    /*package*/ static final String PARAMS_TOP_TO_STRING = "__@RegexTop__";

    /**
     * Create and set the top of the qual params hierarchy
     */
    private void setTop(final QualifierHierarchy<PolyQual<Q>> polyQualHierarchy) {
        PARAMS_TOP = new QualParams<Q>(polyQualHierarchy.getTop()) {
            public String toString() {
                return PARAMS_TOP_TO_STRING;
            }};
    }

    /**
     * Create and set the bottom of the qual params hierarchy
     */
    private void setBottom(final QualifierHierarchy<PolyQual<Q>> polyQualHierarchy) {
        PARAMS_BOTTOM = new QualParams<Q>(polyQualHierarchy.getBottom()) {
            public String toString() {
                return PARAMS_BOTTOM_TO_STRING;
            }};
    }

    @Override
    public QualParams<Q> getBottom() {
        return PARAMS_BOTTOM;
    }

    @Override
    public QualParams<Q> getTop() {
        return PARAMS_TOP;
    }
}

