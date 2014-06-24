package org.checkerframework.qualframework.poly;

import org.checkerframework.qualframework.base.QualifierHierarchy;

/** A qualifier hierarchy for {@link PolyQual} instances.  This class extends
 * the underlying subtyping hierarchy with qualifier variables, which are
 * handled like Java type variables (JLS 4.10.2 - a type variable is a subtype
 * of its upper bound and a supertype of its lower bound).
 */
public class PolyQualHierarchy<Q> implements QualifierHierarchy<PolyQual<Q>> {
    private QualifierHierarchy<Q> groundHierarchy;

    public PolyQualHierarchy(QualifierHierarchy<Q> groundHierarchy) {
        this.groundHierarchy = groundHierarchy;
    }

    @Override
    public boolean isSubtype(PolyQual<Q> subtype, PolyQual<Q> supertype) {
        if (subtype.equals(supertype)) {
            return true;
        }

        Q subMax = subtype.getMaximum();
        Q superMin = supertype.getMinimum();
        return groundHierarchy.isSubtype(subMax, superMin);
    }

    @Override
    public PolyQual<Q> leastUpperBound(PolyQual<Q> a, PolyQual<Q> b) {
        if (isSubtype(a, b)) {
            return b;
        }

        if (isSubtype(b, a)) {
            return a;
        }

        Q aMax = a.getMaximum();
        Q bMax = b.getMaximum();
        Q groundLub = groundHierarchy.leastUpperBound(aMax, bMax);

        return new PolyQual.GroundQual<Q>(groundLub);
    }

    @Override
    public PolyQual<Q> greatestLowerBound(PolyQual<Q> a, PolyQual<Q> b) {
        if (isSubtype(a, b)) {
            return a;
        }

        if (isSubtype(b, a)) {
            return b;
        }

        Q aMin = a.getMinimum();
        Q bMin = b.getMinimum();
        Q groundGlb = groundHierarchy.greatestLowerBound(aMin, bMin);

        return new PolyQual.GroundQual<Q>(groundGlb);
    }

    @Override
    public PolyQual<Q> getTop() {
        return new PolyQual.GroundQual<Q>(groundHierarchy.getTop());
    }

    @Override
    public PolyQual<Q> getBottom() {
        return new PolyQual.GroundQual<Q>(groundHierarchy.getBottom());
    }
}
