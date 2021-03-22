package sideeffectsonly;

public class SideEffectsOnlyField {
    Object a;
    Object b;

    static void test(SideEffectsOnlyField arg) {
        method(arg);
        method3(arg);
        // :: error: argument.type.incompatible
        method2(arg.a);
        method2(arg.b);
    }

    @org.checkerframework.framework.qual.EnsuresQualifier(
            expression = {"#1.a", "#1.b"},
            qualifier =
                    org.checkerframework.framework.testchecker.sideeffectsonly.qual
                            .SideEffectsOnlyToyBottom.class)
    // :: error: contracts.postcondition.not.satisfied
    static void method(SideEffectsOnlyField x) {}

    @org.checkerframework.dataflow.qual.SideEffectsOnly("#1.a")
    static void method3(SideEffectsOnlyField z) {}

    @org.checkerframework.dataflow.qual.SideEffectFree
    static void method2(
            @org.checkerframework.framework.testchecker.sideeffectsonly.qual
                            .SideEffectsOnlyToyBottom
                    Object x) {}
}
