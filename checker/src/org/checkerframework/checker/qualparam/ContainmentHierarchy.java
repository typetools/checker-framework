package org.checkerframework.checker.qualparam;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.checkerframework.qualframework.base.QualifierHierarchy;

/** A <code>ContainmentHierarchy&lt;Q&gt;</code> describes the containment
 * relation over qualifier parameter values of type
 * <code>ParamValue&lt;Q&gt;</code>.  We say that "A contains B" if for all
 * possible assignments of concrete types to type variables, the set of types
 * ranged over by the wildcard B is a subset of the set of types ranged over by
 * the wildcard A.  (If A or B is not a wildcard, it is equivalent to a
 * wildcard which ranges over exactly one type.  That is, we treat
 * <code>Integer</code> as if it were the equivalent <code>? extends Integer
 * super Integer</code>.)
 */
public class ContainmentHierarchy<Q> {
    private QualifierHierarchy<Q> baseHierarchy;

    public ContainmentHierarchy(QualifierHierarchy<Q> baseHierarchy) {
        this.baseHierarchy = baseHierarchy;
    }

    /** Returns the <code>QualifierHierarchy</code> that is used to compare
     * concrete qualifiers of type <code>Q</code>.
     */
    protected QualifierHierarchy<Q> getBaseHierarchy() {
        return baseHierarchy;
    }


    /** Check if the first argument is contained in the second argument.
     */
    public boolean isContained(ParamValue<Q> a, ParamValue<Q> b) {
        // Match: _, QualVar
        // Match: _, BaseQual
        // Use getClass instead of instanceof, because instanceof doesn't work
        // with generics.
        if (!b.getClass().equals(WildcardQual.class)) {
            return a.equals(b);
        }

        // Match: _, WildcardQual
        @SuppressWarnings("unchecked")
        WildcardQual<Q> wild = (WildcardQual<Q>)b;

        Q aMin = a.getMinimum().getBase(baseHierarchy);
        Q aMax = a.getMaximum().getBase(baseHierarchy);

        Q wildLower = wild.getLower().getMaximum().getBase(baseHierarchy);
        Q wildUpper = wild.getUpper().getMinimum().getBase(baseHierarchy);

        return baseHierarchy.isSubtype(wildLower, aMin) &&
            baseHierarchy.isSubtype(aMax, wildUpper);
    }

    /** Compute the least upper bound of two <code>ParamValue</code>s.
     */
    public ParamValue<Q> leastUpperBound(ParamValue<Q> a, ParamValue<Q> b) {
        return combine(a, b, false);
    }

    /** Compute the greatest lower bound of two <code>ParamValue</code>s.
     */
    public ParamValue<Q> greatestLowerBound(ParamValue<Q> a, ParamValue<Q> b) {
        return combine(a, b, true);
    }

    /** Combine two <code>ParamValue</code>s using either LUB or GLB.
     */
    protected ParamValue<Q> combine(
            ParamValue<Q> a, ParamValue<Q> b, boolean useGLB) {
        // Match: QualVar, _
        // Match: _, QualVar
        if (a.getClass() == QualVar.class || b.getClass() == QualVar.class) {
            throw new IllegalArgumentException(
                    "don't know how to " + (useGLB ? "GLB" : "LUB") + " variables");
        }

        // Match: (BaseQual | WildcardQual), (BaseQual | WildcardQual)

        Q aMin = a.getMinimum().getBase(baseHierarchy);
        Q aMax = a.getMaximum().getBase(baseHierarchy);

        Q bMin = b.getMinimum().getBase(baseHierarchy);
        Q bMax = b.getMaximum().getBase(baseHierarchy);

        Q minimum;
        Q maximum;

        if (!useGLB) {
            minimum = baseHierarchy.leastUpperBound(aMin, bMin);
            maximum = baseHierarchy.leastUpperBound(aMax, bMax);
        } else {
            minimum = baseHierarchy.greatestLowerBound(aMin, bMin);
            maximum = baseHierarchy.greatestLowerBound(aMax, bMax);
        }

        if (maximum.equals(minimum))
            return new BaseQual<Q>(minimum);
        else
            return new WildcardQual<Q>(minimum, maximum);
    }
}
