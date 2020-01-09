import java.util.Random;
import org.checkerframework.checker.guieffect.qual.PolyUI;
import org.checkerframework.checker.guieffect.qual.PolyUIEffect;
import org.checkerframework.checker.guieffect.qual.PolyUIType;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.guieffect.qual.UIType;

public class AnonInnerDefaults {

    private static Random random;

    private static boolean maybe() {
        return random.nextBoolean();
    }

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

    @PolyUIType
    public static interface ParlyPolyIface {
        @PolyUIEffect
        public void doPolyUIStuff();

        public void doSafeStuff();
    }

    public static interface IndirectPolyIface extends PolyIface {}

    @PolyUIType
    public static interface IPolyIfaceCaller {

        @PolyUIEffect
        public void call(final @PolyUI PolyIface p);
    }

    public PolyIface getSafePolyIface(final UIElement e) {
        // :: error: (return.type.incompatible)
        return new PolyIface() { // Anonymous inner class inference for @UI
            @Override
            public void doStuff() {
                // Safe due to anonymous inner class effect inference
                e.dangerous(); // should be okay
            }
        };
    }

    public @UI PolyIface getUIPolyIface(final UIElement e) {
        return new PolyIface() { // Anonymous inner class inference for @UI
            @Override
            public void doStuff() {
                // Safe due to anonymous inner class effect inference
                e.dangerous(); // should be okay
            }
        };
    }

    public void callSafePolyIface(final PolyIface p) {
        p.doStuff();
    }

    @UIEffect
    public void callUIPolyIface(final @UI PolyIface p) {
        p.doStuff();
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
        @UI PolyIface p2 =
                new PolyIface() {
                    @Override
                    public void doStuff() {
                        // Safe due to anonymous inner class effect inference
                        e.dangerous(); // should be okay
                    }
                };
        PolyIface p3 =
                // :: error: (assignment.type.incompatible)
                new PolyIface() { // Anonymous inner class inference for @UI
                    @Override
                    public void doStuff() {
                        // Safe due to anonymous inner class effect inference
                        e.dangerous(); // should be okay
                    }
                };
        @UI PolyIface p4 =
                new IndirectPolyIface() {
                    @Override
                    public void doStuff() {
                        // Safe due to anonymous inner class effect inference
                        e.dangerous(); // should be okay
                    }
                };
        @UI ParlyPolyIface p5 =
                new ParlyPolyIface() {
                    @Override
                    public void doPolyUIStuff() {
                        // Safe due to anonymous inner class effect inference
                        e.dangerous(); // should be okay
                    }

                    @Override
                    @SafeEffect
                    public void doSafeStuff() {
                        e.repaint();
                    }
                };
        ParlyPolyIface p6 =
                new ParlyPolyIface() {
                    @Override
                    public void doPolyUIStuff() {
                        e.repaint(); // Safe
                    }

                    @Override
                    public void doSafeStuff() {
                        // :: error: (call.invalid.ui)
                        e.dangerous(); // No inference here, just as an invalid call
                    }
                };
        @UI ParlyPolyIface p7 =
                new ParlyPolyIface() {
                    @Override
                    public void doPolyUIStuff() {
                        // Safe due to anonymous inner class effect inference
                        e.dangerous(); // should be okay
                    }

                    @Override
                    @SafeEffect
                    public void doSafeStuff() {
                        // :: error: (call.invalid.ui)
                        e.dangerous(); // No inference here, just as an invalid call
                    }
                };
        callSafePolyIface(
                // :: error: (argument.type.incompatible)
                new PolyIface() { // Anonymous inner class inference for @UI
                    @Override
                    public void doStuff() {
                        // Safe due to anonymous inner class effect inference
                        e.dangerous(); // should be okay
                    }
                });
        callUIPolyIface(
                new PolyIface() { // Anonymous inner class inference for @UI
                    @Override
                    public void doStuff() {
                        // Safe due to anonymous inner class effect inference
                        e.dangerous(); // should be okay
                    }
                });
        callSafePolyIface(getSafePolyIface(e));
        callUIPolyIface(getUIPolyIface(e));
        (new IPolyIfaceCaller() { // Anonymous inner class inference for @UI
                    @Override
                    public void call(final @UI PolyIface p) { // No global inference
                        p.doStuff();
                    }
                })
                .call(
                        new PolyIface() { // Anonymous inner class inference for @UI
                            @Override
                            public void doStuff() {
                                // Safe due to anonymous inner class effect inference
                                e.dangerous(); // should be okay
                            }
                        });
        PolyIface maybeUIInstance =
                // :: error: (assignment.type.incompatible)
                (maybe()
                        ? new PolyIface() { // Anonymous inner class inference for @UI
                            @Override
                            public void doStuff() {
                                // Safe due to anonymous inner class effect inference
                                e.dangerous(); // should be okay
                            }
                        }
                        : new PolyIface() {
                            @Override
                            public void doStuff() {}
                        });
    }
}
