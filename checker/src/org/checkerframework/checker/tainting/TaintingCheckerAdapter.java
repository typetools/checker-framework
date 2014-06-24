package org.checkerframework.checker.tainting;

import org.checkerframework.qualframework.base.CheckerAdapter;

import org.checkerframework.qualframework.poly.QualParams;

public class TaintingCheckerAdapter extends CheckerAdapter<QualParams<Tainting>> {
    public TaintingCheckerAdapter() {
        super(new TaintingChecker());
    }
}
