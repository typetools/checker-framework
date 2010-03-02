package checkers.eclipse.actions;

import checkers.basic.*;
import checkers.igj.*;
import checkers.interning.*;
import checkers.javari.*;
import checkers.nullness.*;

public class CheckerActions{
    private CheckerActions(){
        throw new AssertionError("not to be instantiated");
    }

    public static class NullnessAction extends RunCheckerAction{
        public NullnessAction(){
            super(NullnessChecker.class);
        }
    }

    public static class JavariAction extends RunCheckerAction{
        public JavariAction(){
            super(JavariChecker.class);
        }
    }

    public class InternedAction extends RunCheckerAction{
        public InternedAction(){
            super(InterningChecker.class);
        }
    }

    public class IGJAction extends RunCheckerAction{
        public IGJAction(){
            super(IGJChecker.class);
        }
    }

    public class CustomAction extends RunCheckerAction{
        public CustomAction(){
            super(BasicChecker.class);
        }
    }
}
