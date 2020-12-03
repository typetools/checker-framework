package sideeffectsonly;

import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.testchecker.sideeffectsonly.qual.Refined;

public class SideEffectsTest {
    void test(Object x) {
        method(x);
        method1(x);
        // :: error: argument.type.incompatible
        method2(x);
    }

    @EnsuresQualifier(expression = "#1", qualifier = Refined.class)
    // :: error: contracts.postcondition.not.satisfied
    void method(Object x) {}

    void method1(@Refined Object x) {}

    void method2(@Refined Object x) {}
}
