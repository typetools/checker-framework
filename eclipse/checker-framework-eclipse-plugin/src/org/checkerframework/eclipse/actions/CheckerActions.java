package org.checkerframework.eclipse.actions;

import org.checkerframework.checker.fenum.FenumChecker;
import org.checkerframework.checker.formatter.FormatterChecker;
import org.checkerframework.checker.guieffect.GuiEffectChecker;
import org.checkerframework.checker.i18n.I18nChecker;
import org.checkerframework.checker.i18nformatter.I18nFormatterChecker;
import org.checkerframework.checker.index.IndexChecker;
import org.checkerframework.checker.interning.InterningChecker;
import org.checkerframework.checker.linear.LinearChecker;
import org.checkerframework.checker.lock.LockChecker;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.propkey.PropertyKeyChecker;
import org.checkerframework.checker.regex.RegexChecker;
import org.checkerframework.checker.signature.SignatureChecker;
import org.checkerframework.checker.signedness.SignednessChecker;
import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.checker.units.UnitsChecker;
import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.subtyping.SubtypingChecker;
import org.checkerframework.common.value.ValueChecker;

public class CheckerActions {
    private CheckerActions() {
        throw new AssertionError("not to be instantiated");
    }

    public static class NullnessAction extends RunCheckerAction {
        public NullnessAction() {
            super(NullnessChecker.class.getCanonicalName());
        }
    }

    public static class InterningAction extends RunCheckerAction {
        public InterningAction() {
            super(InterningChecker.class.getCanonicalName());
        }
    }

    public static class LockAction extends RunCheckerAction {
        public LockAction() {
            super(LockChecker.class.getCanonicalName());
        }
    }

    public static class IndexAction extends RunCheckerAction {
        public IndexAction() {
            super(IndexChecker.class.getCanonicalName());
        }
    }

    public static class FenumAction extends RunCheckerAction {
        public FenumAction() {
            super(FenumChecker.class.getCanonicalName());
        }
    }

    public static class TaintingAction extends RunCheckerAction {
        public TaintingAction() {
            super(TaintingChecker.class.getCanonicalName());
        }
    }

    public static class RegexAction extends RunCheckerAction {
        public RegexAction() {
            super(RegexChecker.class.getCanonicalName());
        }
    }

    public static class FormatterAction extends RunCheckerAction {
        public FormatterAction() {
            super(FormatterChecker.class.getCanonicalName());
        }
    }

    public static class I18nFormatterAction extends RunCheckerAction {
        public I18nFormatterAction() {
            super(I18nFormatterChecker.class.getCanonicalName());
        }
    }

    public static class PropertyFileAction extends RunCheckerAction {
        public PropertyFileAction() {
            super(PropertyKeyChecker.class.getCanonicalName());
        }
    }

    public static class I18nAction extends RunCheckerAction {
        public I18nAction() {
            super(I18nChecker.class.getCanonicalName());
        }
    }

    public static class SignatureAction extends RunCheckerAction {
        public SignatureAction() {
            super(SignatureChecker.class.getCanonicalName());
        }
    }

    public static class GuiEffectAction extends RunCheckerAction {
        public GuiEffectAction() {
            super(GuiEffectChecker.class.getCanonicalName());
        }
    }

    public static class UnitAction extends RunCheckerAction {
        public UnitAction() {
            super(UnitsChecker.class.getCanonicalName());
        }
    }

    public static class SignednessAction extends RunCheckerAction {
        public SignednessAction() {
            super(SignednessChecker.class.getCanonicalName());
        }
    }

    public static class ConstantValueAction extends RunCheckerAction {
        public ConstantValueAction() {
            super(ValueChecker.class.getCanonicalName());
        }
    }

    public static class AliasingAction extends RunCheckerAction {
        public AliasingAction() {
            super(AliasingChecker.class.getCanonicalName());
        }
    }

    public static class LinearAction extends RunCheckerAction {
        public LinearAction() {
            super(LinearChecker.class.getCanonicalName());
        }
    }

    public static class SubtypingAction extends RunCheckerAction {
        public SubtypingAction() {
            super(SubtypingChecker.class.getCanonicalName());
        }
    }

    public static class CurrentAction extends RunCheckerAction {
        public CurrentAction() {
            super();
        }
    }

    public static class CustomAction extends RunCheckerAction {
        public CustomAction() {
            useCustom = true;
            usePrefs = false;
        }
    }

    public static class SingleCustomAction extends RunCheckerAction {
        public SingleCustomAction() {
            useSingleCustom = true;
            usePrefs = false;
        }
    }
}
