package org.checkerframework.checker.tainting;

import org.checkerframework.qualframework.base.QualifierHierarchy;

import static org.checkerframework.checker.tainting.Tainting.*;

public class TaintingQualifierHierarchy implements QualifierHierarchy<Tainting> {
    @Override
    public boolean isSubtype(Tainting a, Tainting b) {
        return a == UNTAINTED || b == TAINTED;
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
