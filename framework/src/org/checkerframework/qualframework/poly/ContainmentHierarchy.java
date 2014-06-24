package org.checkerframework.qualframework.poly;

import org.checkerframework.qualframework.base.QualifierHierarchy;

/** A hierarchy of {@link Wildcard}s, ordered by the containment relation (JLS
 * 4.5.1).
 */
public class ContainmentHierarchy<Q> implements QualifierHierarchy<Wildcard<Q>> {
    private QualifierHierarchy<PolyQual<Q>> polyQualHierarchy;

    /**
     * @param polyQualHierarchy  the hierarchy to use for comparing wildcard bounds
     */
    public ContainmentHierarchy(QualifierHierarchy<PolyQual<Q>> polyQualHierarchy) {
        this.polyQualHierarchy = polyQualHierarchy;
    }

    @Override
    public boolean isSubtype(Wildcard<Q> subtype, Wildcard<Q> supertype) {
        if (subtype.isEmpty()) {
            return true;
        }

        if (supertype.isEmpty()) {
            return false;
        }

        // We consider SUB to be a subtype of SUPER if SUB is contained by
        // SUPER.  SUPER contains SUB if the bounds of SUPER lie outside the
        // bounds of SUB.
        return polyQualHierarchy.isSubtype(supertype.getLowerBound(), subtype.getLowerBound())
            && polyQualHierarchy.isSubtype(subtype.getUpperBound(), supertype.getUpperBound());
    }

    @Override
    public Wildcard<Q> leastUpperBound(Wildcard<Q> a, Wildcard<Q> b) {
        if (a.isEmpty()) {
            return b;
        }

        if (b.isEmpty()) {
            return a;
        }

        // Take the more permissive input for each bound, to produce a new
        // wildcard that contains both `a` and `b`.
        return new Wildcard<Q>(
                polyQualHierarchy.greatestLowerBound(a.getLowerBound(), b.getLowerBound()),
                polyQualHierarchy.leastUpperBound(a.getUpperBound(), b.getUpperBound()));
    }

    @Override
    public Wildcard<Q> greatestLowerBound(Wildcard<Q> a, Wildcard<Q> b) {
        if (a.isEmpty()) {
            return a;
        }

        if (b.isEmpty()) {
            return b;
        }

        PolyQual<Q> newLower = polyQualHierarchy.leastUpperBound(a.getLowerBound(), b.getLowerBound());
        PolyQual<Q> newUpper = polyQualHierarchy.greatestLowerBound(a.getUpperBound(), b.getUpperBound());

        if (!polyQualHierarchy.isSubtype(newLower, newUpper)) {
            return Wildcard.empty();
        } else {
            return new Wildcard<Q>(newLower, newUpper);
        }
    }

    @Override
    public Wildcard<Q> getTop() {
        return new Wildcard<Q>(polyQualHierarchy.getBottom(), polyQualHierarchy.getTop());
    }

    @Override
    public Wildcard<Q> getBottom() {
        return Wildcard.empty();
    }
}

