package checkers.eclipse.actions;

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
            super(CheckerInfo.NULLNESS_CLASS);
        }
    }

    public static class JavariAction extends RunCheckerAction
    {
        public JavariAction()
        {
            super(CheckerInfo.JAVARI_CLASS);
        }
    }

    public static class InterningAction extends RunCheckerAction
    {
        public InterningAction()
        {
            super(CheckerInfo.INTERNING_CLASS);
        }
    }

    public static class IGJAction extends RunCheckerAction
    {
        public IGJAction()
        {
            super(CheckerInfo.IGJ_CLASS);
        }
    }

    public static class FenumAction extends RunCheckerAction
    {
        public FenumAction()
        {
            super(CheckerInfo.FENUM_CLASS);
        }
    }

    public static class LinearAction extends RunCheckerAction
    {
        public LinearAction()
        {
            super(CheckerInfo.LINEAR_CLASS);
        }
    }

    public static class LockAction extends RunCheckerAction
    {
        public LockAction()
        {
            super(CheckerInfo.LOCK_CLASS);
        }
    }

    public static class TaintingAction extends RunCheckerAction
    {
        public TaintingAction()
        {
            super(CheckerInfo.TAINTING_CLASS);
        }
    }

    public static class I18nAction extends RunCheckerAction
    {
        public I18nAction()
        {
            super(CheckerInfo.I18N_CLASS);
        }
    }

    public static class RegexAction extends RunCheckerAction
    {
        public RegexAction()
        {
            super(CheckerInfo.REGEX_CLASS);
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
}
