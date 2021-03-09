package sideeffectsonly;

public class SideEffectsOnlyEmpty {
    void test(Object x) {
        method(x);
        method1(x);
        // :: error: argument.type.incompatible
        method2(x);
    }

    @org.checkerframework.framework.qual.EnsuresQualifier(
            expression = "#1",
            qualifier =
                    org.checkerframework.framework.testchecker.sideeffectsonly.qual
                            .SideEffectsOnlyToyBottom.class)
    // :: error: contracts.postcondition.not.satisfied
    void method(Object x) {}

    @org.checkerframework.dataflow.qual.SideEffectsOnly({})
    void method1(
            @org.checkerframework.framework.testchecker.sideeffectsonly.qual
                            .SideEffectsOnlyToyBottom
                    Object x) {}

    void method2(
            @org.checkerframework.framework.testchecker.sideeffectsonly.qual
                            .SideEffectsOnlyToyBottom
                    Object x) {}
}
