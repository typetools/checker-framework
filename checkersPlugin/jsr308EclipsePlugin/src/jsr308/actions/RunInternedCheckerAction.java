package jsr308.actions;

import checkers.interning.*;

public class RunInternedCheckerAction extends RunCheckerAction{
    public RunInternedCheckerAction(){
        super(InterningChecker.class);
    }
}
