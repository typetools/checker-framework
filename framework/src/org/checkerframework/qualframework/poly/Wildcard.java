package org.checkerframework.qualframework.poly;

import java.util.*;

/** A qualifier wildcard, bounded above and below by {@link PolyQual}s.
 */
public class Wildcard<Q> {
    private final PolyQual<Q> lower;
    private final PolyQual<Q> upper;

    public Wildcard(PolyQual<Q> lower, PolyQual<Q> upper) {
        // Don't let the user pass in `null`.  We use `null` to represent the
        // special empty wildcard.
        if (lower == null || upper == null) {
            throw new IllegalArgumentException("wildcard bounds may not be null");
        }
        this.lower = lower;
        this.upper = upper;
    }

    public Wildcard(PolyQual<Q> qual) {
        this(qual, qual);
    }

    public Wildcard(Q groundLower, Q groundUpper) {
        this(new PolyQual.GroundQual<Q>(groundLower), new PolyQual.GroundQual<Q>(groundUpper));
    }

    public Wildcard(Q groundQual) {
        this(new PolyQual.GroundQual<Q>(groundQual));
    }


    // Force the user to write `Wildcard.empty()` instead of `new Wildcard()`,
    // to make it clear that they're getting something special, rather than
    // a normal wildcard with default bounds or something like that.
    private Wildcard() {
        this.lower = null;
        this.upper = null;
    }

    /** Produce the empty wildcard. */
    public static <Q> Wildcard<Q> empty() {
        return new Wildcard<Q>();
    }


    /** Get the lower bound of the wildcard, or {@code null} if this is the
     * empty wildcard. */
    public PolyQual<Q> getLowerBound() {
        return this.lower;
    }

    /** Get the upper bound of the wildcard, or {@code null} if this is the
     * empty wildcard. */
    public PolyQual<Q> getUpperBound() {
        return this.upper;
    }

    /** Check if this is the empty wildcard. */
    public boolean isEmpty() {
        return this.lower == null;
    }

    /** Substitute wildcards for qualifier parameters. */
    public Wildcard<Q> substitute(Map<String, Wildcard<Q>> substs) {
        Map<String, PolyQual<Q>> lowerSubsts = new HashMap<>();
        Map<String, PolyQual<Q>> upperSubsts = new HashMap<>();

        for (String k : substs.keySet()) {
            lowerSubsts.put(k, substs.get(k).getLowerBound());
            upperSubsts.put(k, substs.get(k).getUpperBound());
        }

        PolyQual<Q> newLower = lower.substitute(lowerSubsts);
        PolyQual<Q> newUpper = upper.substitute(upperSubsts);
        return new Wildcard<Q>(newLower, newUpper);
    }

    /** Combine with another wildcard, using the provided {@link
     * CombiningOperation}s for the upper and lower bounds.
     */
    public Wildcard<Q> combineWith(Wildcard<Q> other,
            CombiningOperation<Q> lowerOp, CombiningOperation<Q> upperOp) {
        return new Wildcard<Q>(
                this.getLowerBound().combineWith(other.getLowerBound(), lowerOp),
                this.getUpperBound().combineWith(other.getUpperBound(), upperOp));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        Wildcard other = (Wildcard)o;
        return this.lower.equals(other.lower)
            && this.upper.equals(other.upper);
    }

    @Override
    public int hashCode() {
        return this.lower.hashCode() * 37
            + this.upper.hashCode() * 59;
    }

    @Override
    public String toString() {
        if (this.isEmpty()) {
            return "ø";
        } else if (lower.equals(upper)) {
            return lower.toString();
        } else {
            return "? ∈ [" + lower + ".." + upper + "]";
        }
    }
}
