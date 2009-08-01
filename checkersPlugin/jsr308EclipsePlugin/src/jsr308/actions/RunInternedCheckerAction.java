package jsr308.actions;

import checkers.interned.*;

public class RunInternedCheckerAction extends RunCheckerAction{
    public RunInternedCheckerAction(){
        super(InternedChecker.class);
    }
}
