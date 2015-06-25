package org.checkerframework.checker.experimental.regex_qual;

import static org.checkerframework.checker.experimental.regex_qual.Regex.BOTTOM;
import static org.checkerframework.checker.experimental.regex_qual.Regex.TOP;

import org.checkerframework.checker.experimental.regex_qual.Regex.PartialRegex;
import org.checkerframework.checker.experimental.regex_qual.Regex.RegexVal;
import org.checkerframework.qualframework.base.QualifierHierarchy;

/**
 * QualifierHierarchy for the Regex-Qual type system. The Hierarchy consists of
 * RegexTop, RegexBottom, RegexVal and PartialRegex.
 *
 * <ul>
 *   <li>RegexVal and PartialRegex are incomparable.</li>
 *   <li>A PartialRegex is a subtype of another PartialRegex if they have the same partial regex.</li>
 *   <li>A RegexVal is a subtype of another RegexVal with a smaller count.</li>
 * </ul>
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

        if (subtype.isRegexVal() && supertype.isRegexVal()) {
            return ((RegexVal) subtype).getCount() >= ((RegexVal) supertype).getCount();

        } else if (subtype.isPartialRegex() && supertype.isPartialRegex()) {
            return ((PartialRegex) subtype).getPartialValue().equals(((PartialRegex) supertype).getPartialValue());

        } else {
            return false;
        }
    }

    @Override
    public Regex leastUpperBound(Regex a, Regex b) {
        if (a == TOP || b == TOP) {
            return TOP;
        } else if (a == BOTTOM) {
            return b;
        } else if (b == BOTTOM) {
            return a;
        }

        if (a.isRegexVal() && b.isRegexVal()) {
            return new RegexVal(Math.min(((RegexVal) a).getCount(), ((RegexVal) b).getCount()));
        } else if (a.isPartialRegex() && b.isPartialRegex()) {
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
        } else if (a == TOP) {
            return b;
        } else if (b == TOP) {
            return a;
        }

        if (a.isRegexVal() && b.isRegexVal()) {
            return new RegexVal(Math.max(((RegexVal) a).getCount(), ((RegexVal) b).getCount()));
        } else if (a.isPartialRegex() && b.isPartialRegex()) {
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
