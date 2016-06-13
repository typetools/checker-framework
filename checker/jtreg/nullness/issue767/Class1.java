/*
 * @test
 * @summary Test that compliation order doesn't effect typechecking (#767)
 * @ignore Until Issue #767 is fixed
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack -Anomsgtext Class1.java Class2.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack -Anomsgtext Class2.java Class1.java
 *
 */
import org.checkerframework.checker.nullness.qual.*;

public class Class1 {
    public static @Nullable Object field = null;
    public @Nullable Object instanceField = null;

    @EnsuresNonNull("instanceField")
    public void instanceMethod() {
        instanceField = new Object();
    }

    @EnsuresNonNull("Class1.field")
    public static void method() {
        field = new Object();
    }

    @EnsuresNonNull("Class2.field")
    public static void method2() {
        Class2.field = new Object();
    }

    @EnsuresNonNull("#1.instanceField")
    public static void method3(Class2 class2) {
        class2.instanceField = new Object();
    }
}
