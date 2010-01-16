package jsr308.actions;

import checkers.nullness.*;

public class RunNullnessCheckerAction extends RunCheckerAction{
    public RunNullnessCheckerAction(){
        super(NullnessChecker.class);
    }
}
