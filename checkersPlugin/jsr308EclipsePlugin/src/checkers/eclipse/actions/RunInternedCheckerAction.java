package checkers.eclipse.actions;

import checkers.interning.*;

public class RunInternedCheckerAction extends RunCheckerAction{
    public RunInternedCheckerAction(){
        super(InterningChecker.class);
    }
}
