package org.checkerframework.checker.experimental.tainting_qual;

import org.checkerframework.qualframework.base.CheckerAdapter;

public class TaintingCheckerAdapter extends CheckerAdapter<Tainting> {
    public TaintingCheckerAdapter() {
        super(new TaintingChecker());
    }
}
