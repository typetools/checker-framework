package org.checkerframework.checkers.tainting;

import org.checkerframework.framework.base.CheckerAdapter;

public class TaintingCheckerAdapter extends CheckerAdapter<Tainting> {
    public TaintingCheckerAdapter() {
        super(new TaintingChecker());
    }
}
