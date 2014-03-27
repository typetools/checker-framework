package org.checkerframework.checker.tainting;

import org.checkerframework.qualframework.base.CheckerAdapter;

public class TaintingCheckerAdapter extends CheckerAdapter<Tainting> {
    public TaintingCheckerAdapter() {
        super(new TaintingChecker());
    }
}
