package sideeffectsonly;

import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.qual.EnsuresQualifier;

public class SideEffectsOnlyTest {
    void test(Object x) {
        method(x);
        method1(x);
        method3(x);
        method2(x);
        // :: error: argument.type.incompatible
        method3(x);
    }

    @EnsuresQualifier(
            expression = "#1",
            qualifier =
                    org.checkerframework.framework.testchecker.sideeffectsonly.qual
                            .SideEffectsOnlyToyBottom.class)
    // :: error: contracts.postcondition.not.satisfied
    void method(Object x) {}

    @SideEffectsOnly({"this"})
    void method1(
            @org.checkerframework.framework.testchecker.sideeffectsonly.qual
                            .SideEffectsOnlyToyBottom
                    Object x) {}

    @SideEffectsOnly({"#1"})
    void method2(
            @org.checkerframework.framework.testchecker.sideeffectsonly.qual
                            .SideEffectsOnlyToyBottom
                    Object x) {}

    @SideEffectsOnly({"this"})
    void method3(
            @org.checkerframework.framework.testchecker.sideeffectsonly.qual
                            .SideEffectsOnlyToyBottom
                    Object x) {}
}
