import org.checkerframework.common.util.report.qual.*;

public class Overrides {
    class A {
        void m() {}
    }

    class B extends A {
        @ReportOverride
        void m() {}
    }

    class C extends B {
        // :: error: (override)
        void m() {}
    }

    // No explicit override -> no message.
    class D extends B {}

    class E extends A {
        // Overrides method on same level as B.m
        // -> no message.
        void m() {}
    }
}
