package org.checkerframework.checker.qualparam;

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

    public static <Q> Wildcard<Q> empty() {
        return new Wildcard<Q>();
    }


    public PolyQual<Q> getLowerBound() {
        return this.lower;
    }

    public PolyQual<Q> getUpperBound() {
        return this.upper;
    }

    public boolean isEmpty() {
        return this.lower == null;
    }

    public Wildcard<Q> substitute(String name, Wildcard<Q> value) {
        PolyQual<Q> newLower = lower.substitute(name, value.getLowerBound());
        PolyQual<Q> newUpper = upper.substitute(name, value.getUpperBound());
        return new Wildcard<Q>(newLower, newUpper);
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
            return "(? ∈ [" + lower + ".." + upper + "])";
        }
    }
}
