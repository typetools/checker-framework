import org.checkerframework.checker.guieffect.qual.UIEffect;

public class TransitiveInheritance {

    public static class TopLevel {
        // Implicitly safe
        public void foo() {}
    }

    public static interface ITop {
        public void bar();
    }

    // Mid-level class and interface that do not redeclare or override the default safe methods
    public static class MidLevel extends TopLevel {}

    public static interface IMid extends ITop {}

    // Issue #3287 is that if foo or bar is overridden with a @UIEffect implementation here, the
    // "skip" in declarations causes the override error to not be issued
    // We check both classes and interfaces
    public static class Base extends MidLevel implements IMid {
        @Override
        @UIEffect
        // :: error: (override.effect.invalid)
        public void foo() {}

        @Override
        @UIEffect
        // :: error: (override.effect.invalid)
        public void bar() {}
    }

    public static interface IBase extends IMid {
        @Override
        @UIEffect
        // :: error: (override.effect.invalid)
        public void bar();
    }
}
