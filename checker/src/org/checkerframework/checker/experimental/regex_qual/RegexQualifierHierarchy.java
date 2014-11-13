package org.checkerframework.checker.experimental.regex_qual;

import org.checkerframework.qualframework.base.QualifierHierarchy;

import static org.checkerframework.checker.experimental.regex_qual.Regex.BOTTOM;
import static org.checkerframework.checker.experimental.regex_qual.Regex.RegexVal;
import static org.checkerframework.checker.experimental.regex_qual.Regex.TOP;

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

        if (subtype instanceof Regex.RegexVal && supertype instanceof Regex.RegexVal) {
            return ((Regex.RegexVal) subtype).getCount() >= ((Regex.RegexVal) supertype).getCount();

        } else if (subtype instanceof Regex.PartialRegex && supertype instanceof Regex.PartialRegex) {
            return ((Regex.PartialRegex) subtype).getPartialValue().equals(((Regex.PartialRegex) supertype).getPartialValue());
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

        if (a instanceof Regex.RegexVal && b instanceof Regex.RegexVal) {
            return new RegexVal(Math.min(((Regex.RegexVal) a).getCount(), ((Regex.RegexVal) b).getCount()));
        } else if (a instanceof Regex.PartialRegex && b instanceof Regex.PartialRegex) {
            if (((Regex.PartialRegex) a).getPartialValue().equals(((Regex.PartialRegex) b).getPartialValue())) {
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

        if (a instanceof Regex.RegexVal && b instanceof Regex.RegexVal) {
            return new RegexVal(Math.max(((Regex.RegexVal) a).getCount(), ((Regex.RegexVal) b).getCount()));
        } else if (a instanceof Regex.PartialRegex && b instanceof Regex.PartialRegex) {
            if (((Regex.PartialRegex) a).getPartialValue().equals(((Regex.PartialRegex) b).getPartialValue())) {
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
