/*
 * @test
 * @summary Test that the stub files get invoked
 * @compile/fail/ref=NonDefaultConstructor.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Alint NonDefaultConstructor.java
 */

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class NonDefaultConstructor {
    Object nonNull = 4;
    Object nullObject;
    @MonotonicNonNull Object lazyField;

    // error doesn't initialize nullObject
    public NonDefaultConstructor() {}

    // error doesn't initialize nullObject
    public NonDefaultConstructor(int i) {
        lazyField = "m";
    }

    // OK, lazyField is lazy!
    public NonDefaultConstructor(double a) {
        nullObject = "n";
    }

    public NonDefaultConstructor(String s) {
        nullObject = "a";
        lazyField = "m";
    }

    public Object getNull() {
        return nullObject;
    }
}
