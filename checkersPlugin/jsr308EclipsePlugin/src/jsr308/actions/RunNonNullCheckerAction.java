package jsr308.actions;

import checkers.nullness.*;

public class RunNonNullCheckerAction extends RunCheckerAction{
    public RunNonNullCheckerAction(){
        super(NullnessChecker.class);
    }
}
