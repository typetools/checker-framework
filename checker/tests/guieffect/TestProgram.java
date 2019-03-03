import org.checkerframework.checker.guieffect.qual.AlwaysSafe;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import packagetests.SafeByDecl;
import packagetests.UIByPackageDecl;

public class TestProgram {
    public void nonUIStuff(
            final UIElement e,
            final GenericTaskUIConsumer uicons,
            final GenericTaskSafeConsumer safecons) {
        // :: error: (call.invalid.ui)
        e.dangerous(); // should be bad
        e.runOnUIThread(
                new IAsyncUITask() {
                    final UIElement e2 = e;

                    @Override
                    public void doStuff() { // should inherit UI effect
                        e2.dangerous(); // should be okay
                    }
                });
        uicons.runAsync(
                // :: error: (declaration.inconsistent.with.implements.clause)
                new @UI IGenericTask() {
                    final UIElement e2 = e;

                    @Override
                    public void doGenericStuff() { // Should be inst. w/ @UI eff.
                        e2.dangerous(); // should be okay
                    }
                });
        safecons.runAsync(
                new @AlwaysSafe IGenericTask() {
                    final UIElement e2 = e;

                    @Override
                    public void doGenericStuff() { // Should be inst. w/ @AlwaysSafe
                        // :: error: (call.invalid.ui)
                        e2.dangerous(); // should be an error
                        safecons.runAsync(this); // Should be okay, this:@AlwaysSafe
                    }
                });
        safecons.runAsync(
                // :: error: (argument.type.incompatible) :: error:
                // (declaration.inconsistent.with.implements.clause)
                new @UI IGenericTask() {
                    final UIElement e2 = e;

                    @Override
                    public void doGenericStuff() { // Should be inst. w/ @UI
                        e2.dangerous(); // should be ok
                        // :: error: (argument.type.incompatible)
                        safecons.runAsync(this); // Should be error, this:@UI
                    }
                });
        // Test that the package annotation works
        // :: error: (call.invalid.ui)
        UIByPackageDecl.implicitlyUI();
        // Test that @SafeType works: SafeByDecl is inside a @UIPackage
        SafeByDecl.safeByTypeDespiteUIPackage();
        safecons.runAsync(
                // :: error: (argument.type.incompatible)
                new IGenericTask() {
                    @Override
                    public void doGenericStuff() {
                        // Safe here due to anonymous inner class effect inference, but will trigger
                        // an error above due to safecons.runAsync not taking an @UI IGenericTask.
                        UIByPackageDecl.implicitlyUI();
                    }
                });
        safecons.runAsync(
                new IGenericTask() {
                    @Override
                    @UIEffect
                    // :: error: (override.effect.invalid.nonui)
                    public void doGenericStuff() {
                        UIByPackageDecl.implicitlyUI();
                    }
                });
    }
}
