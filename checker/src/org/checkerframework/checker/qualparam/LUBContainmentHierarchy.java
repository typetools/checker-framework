package org.checkerframework.checker.qualparam;

import java.util.*;

import org.checkerframework.qualframework.base.QualifierHierarchy;

/** A <code>ContainmentHierarchy</code> that supports least upper bound
 * operations involving qualifier variables.  LUB operations involving
 * variables produce instances of <code>LUBQual</code>.
 */
public class LUBContainmentHierarchy<Q> extends ContainmentHierarchy<Q> {
    public LUBContainmentHierarchy(QualifierHierarchy<Q> baseHierarchy) {
        super(baseHierarchy);
    }


    public boolean isContained(ParamValue<Q> a, ParamValue<Q> b) {
        if (!b.getClass().equals(LUBQual.class)) {
            return super.isContained(a, b);
        }

        // Match: _, LUBQual

        LUBQual<Q> bLub = (LUBQual<Q>)b;

        // Construct a LUBQual equivalent to 'a'.
        if (a instanceof QualVar) {
            Set<QualVar<Q>> vars = new HashSet<>();
            vars.add((QualVar<Q>)a);
            a = new LUBQual<Q>(this, vars);
        }

        if (a instanceof LUBQual) {
            LUBQual<Q> aLub = (LUBQual<Q>)a;
            // Cancel out common variables between aLub and bLub.
            Set<QualVar<Q>> newAVars = new HashSet<>(aLub.getVariables());
            newAVars.removeAll(bLub.getVariables());

            Set<QualVar<Q>> newBVars = new HashSet<>(bLub.getVariables());
            newBVars.removeAll(aLub.getVariables());

            a = new LUBQual<Q>(this, aLub.getConcrete(), newAVars);
            bLub = new LUBQual<Q>(this, bLub.getConcrete(), newBVars);
        }

        // Check that 'a' is contained in 'b' for all possible assignments to
        // variables.
        Q bLower = bLub.calcLowerBound().getBase(getBaseHierarchy());
        Q bUpper = bLub.calcUpperBound().getBase(getBaseHierarchy());

        Q aMax = a.getMaximum().getBase(getBaseHierarchy());
        Q aMin = a.getMinimum().getBase(getBaseHierarchy());

        return getBaseHierarchy().isSubtype(aMax, bUpper)
            && getBaseHierarchy().isSubtype(bLower, aMin);
    }

    public ParamValue<Q> leastUpperBound(ParamValue<Q> a, ParamValue<Q> b) {
        return combine(a, b, false);
    }

    public ParamValue<Q> greatestLowerBound(ParamValue<Q> a, ParamValue<Q> b) {
        return combine(a, b, true);
    }

    protected ParamValue<Q> combine(
            ParamValue<Q> a, ParamValue<Q> b, boolean useGLB) {
        // Match: QualVar, _
        // Match: _, QualVar
        if (a instanceof QualVar || b instanceof QualVar || a instanceof LUBQual || b instanceof LUBQual) {
            if (useGLB) {
                throw new IllegalArgumentException(
                        "don't know how to GLB variables");
            }

            LUBQual<Q> aLub = makeLUBQual(a);
            LUBQual<Q> bLub = makeLUBQual(b);

            Set<QualVar<Q>> newVars = new HashSet<>(aLub.getVariables());
            newVars.addAll(bLub.getVariables());

            ParamValue<Q> newConcrete = leastUpperBound(aLub.getConcrete(), bLub.getConcrete());

            return new LUBQual<>(this, newConcrete, newVars);
        }

        return super.combine(a, b, useGLB);
    }

    // Casts to generic types can't be fully checked at run-time, so they
    // generate warnings.
    @SuppressWarnings("unchecked")
    private LUBQual<Q> makeLUBQual(ParamValue<Q> v) {
        if (v instanceof LUBQual)
            return (LUBQual<Q>)v;

        if (v instanceof QualVar) {
            List<QualVar<Q>> vars = new ArrayList<>();
            vars.add((QualVar<Q>)v);
            return new LUBQual<>(this, vars);
        }

        if (v instanceof BaseQual)
            return new LUBQual<>(this, (BaseQual<Q>)v);

        throw new IllegalArgumentException(
                "don't know how to convert " + v + " to a LUBQual");
    }
}
