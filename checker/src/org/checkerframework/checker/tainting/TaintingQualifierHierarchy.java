package org.checkerframework.checker.tainting;

import static org.checkerframework.checker.tainting.Tainting.*;

import org.checkerframework.qualframework.base.QualifierHierarchy;

public class TaintingQualifierHierarchy implements QualifierHierarchy<Tainting> {
    @Override
    public boolean isSubtype(Tainting subtype, Tainting supertype) {
        return subtype == UNTAINTED || supertype == TAINTED;
    }

    @Override
    public Tainting leastUpperBound(Tainting a, Tainting b) {
        if (a == TAINTED || b == TAINTED) {
            return TAINTED;
        } else {
            return UNTAINTED;
        }
    }

    @Override
    public Tainting greatestLowerBound(Tainting a, Tainting b) {
        if (a == TAINTED && b == TAINTED) {
            return TAINTED;
        } else {
            return UNTAINTED;
        }
    }

    @Override
    public Tainting getTop() {
        return TAINTED;
    }

    @Override
    public Tainting getBottom() {
        return UNTAINTED;
    }
}
