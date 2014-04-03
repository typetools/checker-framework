package org.checkerframework.checker.qualparam;

/** A qualifier variable with an upper and lower bound.
 */
public class QualVar<Q> extends ParamValue<Q> {
    private String name;
    private ParamValue<Q> lower;
    private ParamValue<Q> upper;

    public QualVar(String name) {
        this(name, null, null);
    }

    public QualVar(String name, ParamValue<Q> lower, ParamValue<Q> upper) {
        if (lower == null)
            lower = BaseQual.<Q>getBottom();
        if (upper == null)
            upper = BaseQual.<Q>getTop();

        this.name = name;
        this.lower = lower;
        this.upper = upper;
    }

    public String getName() {
        return name;
    }

    public ParamValue<Q> getLower() {
        return lower;
    }

    public ParamValue<Q> getUpper() {
        return upper;
    }

    public ParamValue<Q> substitute(String name, ParamValue<Q> value) {
        if (name.equals(this.name))
            return value;

        // Otherwise, perform substitution on the bounds.

        ParamValue<Q> newLower = lower.substitute(name, value);
        ParamValue<Q> newUpper = upper.substitute(name, value);

        // It's correct to use == here.  For most ParamValue implementations,
        // 'substitute' returns 'this' if there is no substitution to be made.
        if (newLower == lower && newUpper == upper)
            return this;

        return new QualVar<Q>(this.name, newLower, newUpper);
    }

    public ParamValue<Q> capture() {
        ParamValue<Q> newLower = lower.capture();
        ParamValue<Q> newUpper = upper.capture();

        return new QualVar<Q>(this.name, newLower, newUpper);
    }

    public BaseQual<Q> getMinimum() {
        return lower.getMinimum();
    }

    public BaseQual<Q> getMaximum() {
        return upper.getMaximum();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(QualVar.class))
            return false;

        @SuppressWarnings("unchecked")
        QualVar<Q> other = (QualVar<Q>)o;
        return this.name.equals(other.name) &&
            this.lower.equals(other.lower) &&
            this.upper.equals(other.upper);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() * 31 +
            this.lower.hashCode() * 17 + this.upper.hashCode();
    }

    @Override
    public String toString() {
        return name + " extends " + upper + " super " + lower;
    }
}
