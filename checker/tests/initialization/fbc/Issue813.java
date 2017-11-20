// Test case for Issue 813
// https://github.com/typetools/checker-framework/issues/813
// @skip-test

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

class Issue813 {
    static interface MyInterface {}

    static class MyClass {
        MyClass(@UnderInitialization MyInterface stuff) {}
    }

    static class Fails implements MyInterface {
        @NotOnlyInitialized MyClass bar = new MyClass(this);
    }

    static class Works implements MyInterface {
        @NotOnlyInitialized MyClass bar;

        {
            bar = new MyClass(this); // works
        }
    }
}
