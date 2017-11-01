import org.checkerframework.checker.guieffect.qual.PolyUIEffect;
import org.checkerframework.checker.guieffect.qual.PolyUIType;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.guieffect.qual.UIType;

public class AnonInnerDefaults {

    public static interface SafeIface {
        public void doStuff();
    }

    public static interface ExplicitUIIface {
        @UIEffect
        public void doStuff();
    }

    @UIType
    public static interface UITypeIface {
        public void doStuff();
    }

    @PolyUIType
    public static interface PolyIface {
        @PolyUIEffect
        public void doStuff();
    }

    @UIEffect
    public void tryStuff(final UIElement e) {
        SafeIface s =
                new SafeIface() {
                    @Override
                    public void doStuff() {
                        // :: error: (call.invalid.ui)
                        e.dangerous();
                    }
                };
        ExplicitUIIface ex =
                new ExplicitUIIface() {
                    @Override
                    public void doStuff() {
                        e.dangerous(); // should be okay
                    }
                };
        UITypeIface u =
                new UITypeIface() {
                    @Override
                    public void doStuff() {
                        e.dangerous(); // should be okay
                    }
                };
        @UI PolyIface p =
                new @UI PolyIface() {
                    @Override
                    public void doStuff() {
                        e.dangerous(); // should be okay
                    }
                };
        PolyIface p2 =
                new PolyIface() {
                    @Override
                    public void doStuff() {
                        // :: error: (call.invalid.ui)
                        e.dangerous(); // should be okay
                    }
                };
    }
}
