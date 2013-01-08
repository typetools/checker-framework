package checkers.eclipse.actions;

import checkers.fenum.FenumChecker;
import checkers.i18n.I18nChecker;
import checkers.igj.IGJChecker;
import checkers.interning.InterningChecker;
import checkers.javari.JavariChecker;
import checkers.linear.LinearChecker;
import checkers.lock.LockChecker;
import checkers.nullness.NullnessChecker;
import checkers.regex.RegexChecker;
import checkers.tainting.TaintingChecker;

public class CheckerActions
{
    private CheckerActions()
    {
        throw new AssertionError("not to be instantiated");
    }

    public static class CurrentAction extends RunCheckerAction
    {
        public CurrentAction()
        {
            super();
        }
    }

    public static class NullnessAction extends RunCheckerAction
    {
        public NullnessAction()
        {
            super(NullnessChecker.class.getCanonicalName());
        }
    }

    public static class JavariAction extends RunCheckerAction
    {
        public JavariAction()
        {
            super(JavariChecker.class.getCanonicalName());
        }
    }

    public static class InterningAction extends RunCheckerAction
    {
        public InterningAction()
        {
            super(InterningChecker.class.getCanonicalName());
        }
    }

    public static class IGJAction extends RunCheckerAction
    {
        public IGJAction()
        {
            super(IGJChecker.class.getCanonicalName());
        }
    }

    public static class FenumAction extends RunCheckerAction
    {
        public FenumAction()
        {
            super(FenumChecker.class.getCanonicalName());
        }
    }

    public static class LinearAction extends RunCheckerAction
    {
        public LinearAction()
        {
            super(LinearChecker.class.getCanonicalName());
        }
    }

    public static class LockAction extends RunCheckerAction
    {
        public LockAction()
        {
            super(LockChecker.class.getCanonicalName());
        }
    }

    public static class TaintingAction extends RunCheckerAction
    {
        public TaintingAction()
        {
            super(TaintingChecker.class.getCanonicalName());
        }
    }

    public static class I18nAction extends RunCheckerAction
    {
        public I18nAction()
        {
            super(I18nChecker.class.getCanonicalName());
        }
    }

    public static class RegexAction extends RunCheckerAction
    {
        public RegexAction()
        {
            super(RegexChecker.class.getCanonicalName());
        }
    }

    public static class CustomAction extends RunCheckerAction
    {
        public CustomAction()
        {
            useCustom = true;
            usePrefs = false;
        }
    }
    
    public static class SingleCustomAction extends RunCheckerAction
    {
    	public SingleCustomAction() 
    	{
    		useSingleCustom = true;
    		usePrefs = false;
    	}
    }
}
