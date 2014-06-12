package org.checkerframework.checker.qualparam;

import java.util.*;

import org.checkerframework.javacutil.Pair;

import org.checkerframework.qualframework.base.QualifierHierarchy;

/** This class provides a <code>QualifierHierarchy</code> implementation for
 * sets of qualifier parameters.  Under this hierarchy, A is a subtype of B iff
 * the value of each qualifier parameter in A is contained within the value of
 * the corresponding parameter in B.
 */
public class QualifierParameterHierarchy<Q> implements QualifierHierarchy<QualParams<Q>> {
    private QualifierHierarchy<Wildcard<Q>> containmentHierarchy;
    private List<Pair<Wildcard<Q>, Wildcard<Q>>> constraintTarget = null;

    public final QualParams<Q> PARAMS_BOTTOM = QualParams.<Q>getBottom();
    public final QualParams<Q> PARAMS_TOP = QualParams.<Q>getTop();


    public QualifierParameterHierarchy(QualifierHierarchy<Wildcard<Q>> containmentHierarchy) {
        this.containmentHierarchy = containmentHierarchy;
    }

    public static <Q> QualifierParameterHierarchy<Q> fromContainment(QualifierHierarchy<Wildcard<Q>> containmentHierarchy) {
        return new QualifierParameterHierarchy<>(containmentHierarchy);
    }

    public static <Q> QualifierParameterHierarchy<Q> fromPolyQual(QualifierHierarchy<PolyQual<Q>> polyQualHierarchy) {
        return fromContainment(new ContainmentHierarchy<Q>(polyQualHierarchy));
    }

    public static <Q> QualifierParameterHierarchy<Q> fromGround(QualifierHierarchy<Q> groundHierarchy) {
        return fromPolyQual(new PolyQualHierarchy<Q>(groundHierarchy));
    }

    protected QualifierHierarchy<Wildcard<Q>> getContaintmentHierarchy() {
        return containmentHierarchy;
    }

    public void setConstraintTarget(List<Pair<Wildcard<Q>, Wildcard<Q>>> constraintTarget) {
        this.constraintTarget = constraintTarget;
    }


    public boolean isSubtype(QualParams<Q> subtype, QualParams<Q> supertype) {
        if (subtype.equals(supertype))
            return true;

        if (subtype == PARAMS_BOTTOM || supertype == PARAMS_TOP)
            return true;

        if (subtype == PARAMS_TOP || supertype == PARAMS_BOTTOM ||
                !subtype.keySet().equals(supertype.keySet())) {
            if (constraintTarget == null) {
                return false;
            } else {
                constraintTarget.add(null);
                return true;
            }
        }

        for (String k : subtype.keySet()) {
            if (constraintTarget == null) {
                if (!containmentHierarchy.isSubtype(subtype.get(k), supertype.get(k)))
                    return false;
            } else {
                constraintTarget.add(Pair.of(subtype.get(k), supertype.get(k)));
            }
        }

        return true;
    }

    public QualParams<Q> leastUpperBound(QualParams<Q> a, QualParams<Q> b) {
        if (this.constraintTarget != null) {
            throw new UnsupportedOperationException("unexpected leastUpperBound when generating constraints");
        }

        if (a == PARAMS_BOTTOM)
            return b;

        if (b == PARAMS_BOTTOM)
            return a;

        if (a == PARAMS_TOP || b == PARAMS_TOP)
            return PARAMS_TOP;

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

        return new QualParams<Q>(result);
    }

    public QualParams<Q> greatestLowerBound(QualParams<Q> a, QualParams<Q> b) {
        if (this.constraintTarget != null) {
            throw new UnsupportedOperationException("unexpected leastUpperBound when generating constraints");
        }

        if (a == PARAMS_TOP)
            return b;

        if (b == PARAMS_TOP)
            return a;

        if (a == PARAMS_BOTTOM || b == PARAMS_BOTTOM)
            return PARAMS_BOTTOM;

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

        return new QualParams<Q>(result);
    }

    public QualParams<Q> getBottom() {
        return PARAMS_BOTTOM;
    }

    public QualParams<Q> getTop() {
        return PARAMS_TOP;
    }
}

