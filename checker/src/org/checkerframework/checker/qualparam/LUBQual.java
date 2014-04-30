package org.checkerframework.checker.qualparam;

import java.util.*;

/** A <code>LUBQual</code> value represents the least upper bound of a set of
 * variables (<code>QualVar</code>s) and a single <code>BaseQual</code> or
 * <code>WildcardQual</code>.
 */
public class LUBQual<Q> extends ParamValue<Q> {
    private ParamValue<Q> concrete;
    private Set<QualVar<Q>> vars;

    private LUBContainmentHierarchy<Q> hierarchy;

    public LUBQual(LUBContainmentHierarchy<Q> hierarchy) {

        this(hierarchy, null, null);
    }

    public LUBQual(LUBContainmentHierarchy<Q> hierarchy, ParamValue<Q> concrete) {
        this(hierarchy, concrete, null);
    }

    public LUBQual(LUBContainmentHierarchy<Q> hierarchy, Collection<QualVar<Q>> vars) {
        this(hierarchy, null, vars);
    }

    public LUBQual(LUBContainmentHierarchy<Q> hierarchy, ParamValue<Q> concrete, Collection<QualVar<Q>> vars) {
        this.hierarchy = hierarchy;

        if (concrete != null) {
            if (!BaseQual.class.isInstance(concrete) && !WildcardQual.class.isInstance(concrete))
                throw new IllegalArgumentException("concrete qualfier must be BaseQual or WildcardQual");
            this.concrete = concrete;
        } else {
            this.concrete = BaseQual.<Q>getBottom();
        }
        

        if (vars != null) {
            this.vars = new HashSet<>(vars);
        } else {
            this.vars = new HashSet<>();
        }
    }

    /** Returns the set of variables that are LUBbed together in this
     * <code>LUBQual</code>.
     */
    public Set<QualVar<Q>> getVariables() {
        return Collections.unmodifiableSet(vars);
    }

    /** Returns the <code>BaseQual</code> or <code>WildcardQual</code> that is
     * LUBbed together with the variables in this <code>LUBQual</code>.
     */
    public ParamValue<Q> getConcrete() {
        return concrete;
    }

    public ParamValue<Q> substitute(String name, ParamValue<Q> value) {
        Set<QualVar<Q>> newVars = new HashSet<>();
        ParamValue<Q> newConcrete = concrete;

        for (QualVar<Q> var : vars) {
            ParamValue<Q> newValue = var.substitute(name, value);
            if (newValue instanceof QualVar) {
                newVars.add((QualVar<Q>)newValue);
            } else {
                newConcrete = hierarchy.leastUpperBound(newConcrete, newValue);
            }
        }

        if (newVars.isEmpty())
            return newConcrete;
        else
            return new LUBQual<Q>(hierarchy, newConcrete, newVars);
    }

    public ParamValue<Q> capture() {
        Set<QualVar<Q>> newVars = new HashSet<>(vars);
        ParamValue<Q> newConcrete = concrete.capture();

        if (newConcrete instanceof QualVar) {
            newVars.add((QualVar<Q>)newConcrete);
            return new LUBQual<Q>(hierarchy, newVars);
        } else {
            return this;
        }
    }

    public BaseQual<Q> getMinimum() {
        // LUB of two BaseQuals should always return another BaseQual.
        return (BaseQual<Q>)hierarchy.leastUpperBound(
                concrete.getMinimum(), getVarsMinimum());
    }

    public BaseQual<Q> getMaximum() {
        // LUB of two BaseQuals should always return another BaseQual.
        return (BaseQual<Q>)hierarchy.leastUpperBound(
                concrete.getMaximum(), getVarsMaximum());
    }

    public BaseQual<Q> calcLowerBound() {
        // LUB of two BaseQuals should always return another BaseQual.
        return (BaseQual<Q>)hierarchy.leastUpperBound(
                concrete.getMinimum(), getVarsMaximum());
    }

    public BaseQual<Q> calcUpperBound() {
        // LUB of two BaseQuals should always return another BaseQual.
        return (BaseQual<Q>)hierarchy.leastUpperBound(
                concrete.getMaximum(), getVarsMaximum());
    }

    private BaseQual<Q> getVarsMaximum() {
        // LUB of two BaseQuals should always return another BaseQual.
        BaseQual<Q> result = BaseQual.<Q>getBottom();
        for (QualVar<Q> var : vars) {
            result = (BaseQual<Q>)hierarchy.leastUpperBound(result, var.getMaximum());
        }
        return result;
    }

    private BaseQual<Q> getVarsMinimum() {
        // LUB of two BaseQuals should always return another BaseQual.
        BaseQual<Q> result = BaseQual.<Q>getBottom();
        for (QualVar<Q> var : vars) {
            result = (BaseQual<Q>)hierarchy.leastUpperBound(result, var.getMinimum());
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(LUBQual.class))
            return false;

        @SuppressWarnings("unchecked")
        LUBQual<Q> other = (LUBQual<Q>)o;
        return this.concrete.equals(other.concrete) &&
            this.vars.equals(other.vars);
    }

    @Override
    public int hashCode() {
        return this.concrete.hashCode() * 17 + this.vars.hashCode();
    }

    @Override
    public String toString() {
        return "LUB(" + vars + "; " + concrete + ")";
    }
}

