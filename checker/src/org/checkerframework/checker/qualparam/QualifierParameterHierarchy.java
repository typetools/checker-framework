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
    private ContainmentHierarchy<Q> containmentHierarchy;
    private List<Pair<ParamValue<Q>, ParamValue<Q>>> constraintTarget = null;

    public final QualParams<Q> PARAMS_BOTTOM = QualParams.<Q>getBottom();
    public final QualParams<Q> PARAMS_TOP = QualParams.<Q>getTop();


    public QualifierParameterHierarchy(QualifierHierarchy<Q> baseHierarchy) {
        this.containmentHierarchy = new ContainmentHierarchy<Q>(baseHierarchy);
    }

    // Allow clients to provied a custom ContainmentHierarchy, so they can add
    // support for new ParamValue subtypes.
    public QualifierParameterHierarchy(ContainmentHierarchy<Q> containmentHierarchy) {
        this.containmentHierarchy = containmentHierarchy;
    }

    protected ContainmentHierarchy<Q> getContaintmentHierarchy() {
        return containmentHierarchy;
    }

    public void setConstraintTarget(List<Pair<ParamValue<Q>, ParamValue<Q>>> constraintTarget) {
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
                if (!containmentHierarchy.isContained(subtype.get(k), supertype.get(k)))
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

        if (!a.keySet().equals(b.keySet()))
            throw new IllegalArgumentException(
                    "tried to LUB two maps with different params defined");

        Map<String, ParamValue<Q>> result = new HashMap<>();
        for (String k : a.keySet()) {
            result.put(k, containmentHierarchy.leastUpperBound(a.get(k), b.get(k)));
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

        if (!a.keySet().equals(b.keySet()))
            throw new IllegalArgumentException(
                    "tried to GLB two maps with different params defined");

        Map<String, ParamValue<Q>> result = new HashMap<>();
        for (String k : a.keySet()) {
            result.put(k, containmentHierarchy.greatestLowerBound(a.get(k), b.get(k)));
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

