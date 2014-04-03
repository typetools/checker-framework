package org.checkerframework.checker.qualparam;

/** A wildcard qualifier with an upper and lower bound.
 */
public class WildcardQual<Q> extends ParamValue<Q> {
    private ParamValue<Q> lower;
    private ParamValue<Q> upper;

    public WildcardQual() {
        this((ParamValue<Q>)null, (ParamValue<Q>)null);
    }

    public WildcardQual(Q lower, Q upper) {
        this(lower == null ? null : new BaseQual<Q>(lower),
                upper == null ? null : new BaseQual<Q>(upper));
    }

    public WildcardQual(ParamValue<Q> lower, ParamValue<Q> upper) {
        if (lower == null)
            lower = BaseQual.<Q>getBottom();
        if (upper == null)
            upper = BaseQual.<Q>getTop();

        this.lower = lower;
        this.upper = upper;
    }

    public ParamValue<Q> getLower() {
        return lower;
    }

    public ParamValue<Q> getUpper() {
        return upper;
    }

    public ParamValue<Q> substitute(String name, ParamValue<Q> value) {
        ParamValue<Q> newLower = lower.substitute(name, value);
        ParamValue<Q> newUpper = upper.substitute(name, value);

        if (newLower == lower && newUpper == upper)
            return this;

        return new WildcardQual<Q>(newLower, newUpper);
    }

    private static int captureCounter = 0;

    public ParamValue<Q> capture() {
        String varName = "CAP#" + (captureCounter++);
        ParamValue<Q> newLower = lower.capture();
        ParamValue<Q> newUpper = upper.capture();

        return new QualVar<Q>(varName, newLower, newUpper);
    }

    public BaseQual<Q> getMinimum() {
        return lower.getMinimum();
    }

    public BaseQual<Q> getMaximum() {
        return upper.getMaximum();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(WildcardQual.class))
            return false;

        @SuppressWarnings("unchecked")
        WildcardQual<Q> other = (WildcardQual<Q>)o;
        return this.lower.equals(other.lower) &&
            this.upper.equals(other.upper);
    }

    @Override
    public int hashCode() {
        return this.lower.hashCode() * 17 + this.upper.hashCode();
    }

    @Override
    public String toString() {
        return "?" +
            (upper != BaseQual.<Q>getTop() ? " extends " + upper : "") + 
            (lower != BaseQual.<Q>getBottom() ? " super " + lower : "");
    }
}

