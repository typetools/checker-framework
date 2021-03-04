package sideeffectsonly;

import org.checkerframework.framework.qual.EnsuresQualifier;

public class SideEffectsTest {
    void test(Object x) {
        method(x);
        method1(x);
        // :: error: argument.type.incompatible
        method2(x);
    }

    @EnsuresQualifier(
            expression = "#1",
            qualifier =
                    org.checkerframework.framework.testchecker.sideeffectsonly.qual
                            .SideEffectsOnlyToyBottom.class)
    // :: error: contracts.postcondition.not.satisfied
    void method(Object x) {}

    void method1(
            @org.checkerframework.framework.testchecker.sideeffectsonly.qual
                            .SideEffectsOnlyToyBottom
                    Object x) {}

    void method2(
            @org.checkerframework.framework.testchecker.sideeffectsonly.qual
                            .SideEffectsOnlyToyBottom
                    Object x) {}
}
