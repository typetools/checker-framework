// Test case for Issue 1046:
// https://github.com/typetools/checker-framework/issues/1046
// Additonal test case: checker/tests/nullness/Issue1046.java
// @below-java8-jdk-skip-test

import java.util.List;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;

class Issue1046 {
    interface EnumMarker {}

    enum MyEnum implements EnumMarker {
        A,
        B;
    }

    static class NS2Lists {
        @SuppressWarnings("nullness")
        static <F, T> List<T> transform(List<F> p, Function<? super F, ? extends T> q) {
            return null;
        }
    }

    abstract class NotSubtype2 {
        void test(List<MyEnum> p) {
            NS2Lists.transform(p, foo());
        }

        abstract Function<? super @UnknownKeyFor EnumMarker, Number> foo();
    }
}
