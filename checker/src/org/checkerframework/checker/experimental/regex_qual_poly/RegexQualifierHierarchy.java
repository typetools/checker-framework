package org.checkerframework.checker.experimental.regex_qual_poly;

import org.checkerframework.qualframework.base.QualifierHierarchy;

import static org.checkerframework.checker.experimental.regex_qual_poly.Regex.BOTTOM;
import static org.checkerframework.checker.experimental.regex_qual_poly.Regex.PartialRegex;
import static org.checkerframework.checker.experimental.regex_qual_poly.Regex.RegexVal;
import static org.checkerframework.checker.experimental.regex_qual_poly.Regex.TOP;

/**
 * QualifierHierarchy for the Regex type system.
 *
 * The Hierarchy consists of RegexTop, RegexBottom, RegexVal and PartialRegex.
 *
 * RegexVal and PartialRegex are incomparable.
 * Partial Regexs are subtypes of each other if they have the same partial regex.
 *
 * A RegexVal is a subtype of another RegexVal with a smaller count.
 *
 */
public class RegexQualifierHierarchy implements QualifierHierarchy<Regex> {

    @Override
    public boolean isSubtype(Regex subtype, Regex supertype) {

        if (supertype == TOP) {
            return true;
        } else if (subtype == TOP) {
            return false;
        } else if (subtype == BOTTOM) {
            return true;
        } else if (supertype == BOTTOM) {
            return false;
        }

        if (subtype instanceof RegexVal && supertype instanceof RegexVal) {
            return ((RegexVal) subtype).getCount() >= ((RegexVal) supertype).getCount();

        } else if (subtype instanceof PartialRegex && supertype instanceof PartialRegex) {
            return ((PartialRegex) subtype).getPartialValue().equals(((PartialRegex) supertype).getPartialValue());
        } else {
            return false;
        }
    }

    @Override
    public Regex leastUpperBound(Regex a, Regex b) {
        if (a == TOP || b == TOP) {
            return TOP;
        } else if (a == BOTTOM && b == BOTTOM) {
            return BOTTOM;
        } else if (a == BOTTOM) {
            return b;
        } else if (b == BOTTOM) {
            return a;
        }

        if (a instanceof RegexVal && b instanceof RegexVal) {
            return new RegexVal(Math.min(((RegexVal) a).getCount(), ((RegexVal) b).getCount()));
        } else if (a instanceof PartialRegex && b instanceof PartialRegex) {
            if (((PartialRegex) a).getPartialValue().equals(((PartialRegex) b).getPartialValue())) {
                return a;
            } else {
                return TOP;
            }
        } else {
            return TOP;
        }
    }

    @Override
    public Regex greatestLowerBound(Regex a, Regex b) {
        if (a == BOTTOM || b == BOTTOM) {
            return BOTTOM;
        } else if (a == TOP && b == TOP) {
            return TOP;
        } else if (a == TOP) {
            return b;
        } else if (b == TOP) {
            return a;
        }

        if (a instanceof RegexVal && b instanceof RegexVal) {
            return new RegexVal(Math.max(((RegexVal) a).getCount(), ((RegexVal) b).getCount()));
        } else if (a instanceof PartialRegex && b instanceof PartialRegex) {
            if (((PartialRegex) a).getPartialValue().equals(((PartialRegex) b).getPartialValue())) {
                return a;
            } else {
                return BOTTOM;
            }
        } else {
            return BOTTOM;
        }
    }

    @Override
    public Regex getTop() {
        return Regex.TOP;
    }

    @Override
    public Regex getBottom() {
        return Regex.BOTTOM;
    }
}
