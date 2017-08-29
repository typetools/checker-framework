import org.checkerframework.checker.guieffect.qual.PolyUI;
import org.checkerframework.checker.guieffect.qual.PolyUIEffect;
import org.checkerframework.checker.guieffect.qual.PolyUIType;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

public class Java8Lambdas {

    private interface SafeFunctionalInterface<T> {
        public void executeAlwaysSafe(T arg);
    }

    private interface UIFunctionalInterface<T> {
        @UIEffect
        public void executeUI(T arg);
    }

    @PolyUIType
    private interface PolymorphicFunctionalInterface<T> {
        @PolyUIEffect
        public void executePolymorphic(T arg);
    }

    private static class LambdaRunner {
        private final UIElement arg;

        public LambdaRunner(UIElement arg) {
            this.arg = arg;
        }

        public void doSafe(SafeFunctionalInterface<UIElement> func) {
            func.executeAlwaysSafe(this.arg);
        }

        @UIEffect
        public void doUI(UIFunctionalInterface<UIElement> func) {
            func.executeUI(this.arg);
        }

        // Needs to be @UIEffect, because the functional interface method is @UIEffect
        public void unsafeDoUI(UIFunctionalInterface<UIElement> func) {
            //:: error: (call.invalid.ui)
            func.executeUI(this.arg);
        }

        public void doEither(@PolyUI PolymorphicFunctionalInterface<UIElement> func) {
            // In a real program some intelligent dispatch could be done here to avoid running on UI thread unless
            // needed.
            arg.runOnUIThread(
                    new IAsyncUITask() {
                        final UIElement e2 = arg;

                        @Override
                        public void doStuff() { // should inherit UI effect
                            func.executePolymorphic(e2); // should be okay
                        }
                    });
        }

        public void doUISafely(@UI PolymorphicFunctionalInterface<UIElement> func) {
            // In a real program some intelligent dispatch could be done here to avoid running on UI thread unless
            // needed.
            arg.runOnUIThread(
                    new IAsyncUITask() {
                        final UIElement e2 = arg;

                        @Override
                        public void doStuff() { // should inherit UI effect
                            func.executePolymorphic(e2); // should be okay
                        }
                    });
        }
    }

    @PolyUIType
    private static class PolymorphicLambdaRunner {
        private final UIElement arg;

        public PolymorphicLambdaRunner(UIElement arg) {
            this.arg = arg;
        }

        @PolyUIEffect
        public void doEither(@PolyUI PolymorphicFunctionalInterface<UIElement> func) {
            func.executePolymorphic(this.arg);
        }
    }

    public static void safeContextTestCases(UIElement elem) {
        LambdaRunner runner = new LambdaRunner(elem);
        runner.doSafe(e -> e.repaint());
        //:: error: (call.invalid.ui)
        runner.doSafe(e -> e.dangerous()); // Not allowed in doSafe
        //:: error: (call.invalid.ui)
        runner.doUI(e -> e.repaint()); // Not allowed in safe context
        //:: error: (call.invalid.ui)
        runner.doUI(e -> e.dangerous()); // Not allowed in safe context
        runner.doEither(e -> e.repaint());
        runner.doEither(e -> e.dangerous());
        runner.doUISafely(e -> e.dangerous());
        PolymorphicLambdaRunner safePolymorphicLambdaRunner = new PolymorphicLambdaRunner(elem);
        safePolymorphicLambdaRunner.doEither(e -> e.repaint());
        //:: error: (call.invalid.ui)
        safePolymorphicLambdaRunner.doEither(e -> e.dangerous());
        @UI PolymorphicLambdaRunner uiPolymorphicLambdaRunner = new @UI PolymorphicLambdaRunner(elem);
        //:: error: (call.invalid.ui)
        uiPolymorphicLambdaRunner.doEither(
                e -> e.repaint()); // Safe at runtime, but not by the type system!
        //:: error: (call.invalid.ui)
        uiPolymorphicLambdaRunner.doEither(e -> e.dangerous());
        PolymorphicFunctionalInterface<UIElement> func1 = e -> e.repaint();
        //:: error: (call.invalid.ui)
        PolymorphicFunctionalInterface<UIElement> func2 = e -> e.dangerous(); // Incompatible types!
        @UI PolymorphicFunctionalInterface<UIElement> func3 = e -> e.dangerous();
        safePolymorphicLambdaRunner.doEither(func1);
        safePolymorphicLambdaRunner.doEither(func2);
        //:: error: (call.invalid.ui)
        uiPolymorphicLambdaRunner.doEither(func1);
        //:: error: (call.invalid.ui)
        uiPolymorphicLambdaRunner.doEither(func2);
        //:: error: (call.invalid.ui)
        uiPolymorphicLambdaRunner.doEither(func3);
    }

    @UIEffect
    public static void uiContextTestCases(UIElement elem) {
        LambdaRunner runner = new LambdaRunner(elem);
        //:: error: (call.invalid.ui)
        runner.doSafe(e -> e.dangerous());
        runner.doUI(e -> e.repaint());
        runner.doUI(e -> e.dangerous());
        PolymorphicLambdaRunner safePolymorphicLambdaRunner = new PolymorphicLambdaRunner(elem);
        //:: error: (call.invalid.ui)
        safePolymorphicLambdaRunner.doEither(e -> e.dangerous());
        @UI PolymorphicLambdaRunner uiPolymorphicLambdaRunner = new @UI PolymorphicLambdaRunner(elem);
        uiPolymorphicLambdaRunner.doEither(e -> e.dangerous());
    }

    public @UI PolymorphicFunctionalInterface<UIElement> returnLambdasTest1() {
        return e -> e.dangerous();
    }

    public PolymorphicFunctionalInterface<UIElement> returnLambdasTest2() {
        return (e -> {
            //:: error: (call.invalid.ui)
            e.dangerous();
        }); // This lambda will be inferred to be safe and thus calling dangerous() is an
        // error.
    }
}
