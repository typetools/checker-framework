package jsr308.actions;

import checkers.nonnull.*;

public class RunNonNullCheckerAction extends RunCheckerAction{
    public RunNonNullCheckerAction(){
        super(NonNullChecker.class);
    }
}
