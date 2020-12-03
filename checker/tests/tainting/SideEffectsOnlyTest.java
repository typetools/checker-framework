package tainting;

import org.checkerframework.checker.tainting.qual.Untainted;

public class SideEffectsOnlyTest {
    void test(@Untainted Object x) {
        method(x);
        method1(x);
    }

    void method(Object x) {}

    void method1(@Untainted Object x) {}
}
