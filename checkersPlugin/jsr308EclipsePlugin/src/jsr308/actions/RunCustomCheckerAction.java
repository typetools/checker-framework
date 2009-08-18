package jsr308.actions;

import checkers.util.*;

public class RunCustomCheckerAction extends RunCheckerAction{
    public RunCustomCheckerAction(){
        super(CustomChecker.class);
    }
}
