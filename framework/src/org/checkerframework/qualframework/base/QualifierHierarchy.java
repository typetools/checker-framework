package org.checkerframework.qualframework.base;

/**
 * {@link DefaultQualifiedTypeFactory} component for performing subtyping
 * checks between qualifiers.
 */
public interface QualifierHierarchy<Q> {
    /** Checks if <code>subtype</code> is a subtype of <code>supertype</code>. */
    boolean isSubtype(Q subtype, Q supertype);

    /** Gets the least upper bound of two qualifiers. */
    Q leastUpperBound(Q a, Q b);
    /** Gets the greatest lower bound of two qualifiers. */
    Q greatestLowerBound(Q a, Q b);

    /** Gets the top annotation of the hierarchy. */
    Q getTop();
    /** Gets the bottom annotation of the hierarchy. */
    Q getBottom();
}
