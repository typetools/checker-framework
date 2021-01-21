// Test case for Issue #818
// https://github.com/typetools/checker-framework/issues/818

import org.checkerframework.checker.nullness.qual.*;

public class Issue818 {
    public static @Nullable Object o = null;

    void method() {
        Issue818.o = new Object();
        o.toString();
    }

    void method2() {
        o = new Object();
        Issue818.o.toString();
    }

    void method3() {
        o = new Object();
        o.toString();
    }

    void method4() {
        Issue818.o = new Object();
        Issue818.o.toString();
    }

    static class StaticInnerClass {
        void method() {
            Issue818.o = new Object();
            o.toString();
        }

        void method2() {
            o = new Object();
            Issue818.o.toString();
        }

        void method3() {
            o = new Object();
            o.toString();
        }

        void method4() {
            Issue818.o = new Object();
            Issue818.o.toString();
        }
    }

    class NonStaticInnerClass {
        void method() {
            Issue818.o = new Object();
            o.toString();
        }

        void method2() {
            o = new Object();
            Issue818.o.toString();
        }

        void method3() {
            o = new Object();
            o.toString();
        }

        void method4() {
            Issue818.o = new Object();
            Issue818.o.toString();
        }
    }
}
