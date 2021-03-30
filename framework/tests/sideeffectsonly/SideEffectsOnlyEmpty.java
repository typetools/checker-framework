package sideeffectsonly;

import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.testchecker.sideeffectsonly.qual.SideEffectsOnlyToyBottom;

public class SideEffectsOnlyEmpty {
    void test(Object x) {
        method(x);
        method1(x);
        // :: error: argument.type.incompatible
        method2(x);
    }

    @EnsuresQualifier(expression = "#1", qualifier = SideEffectsOnlyToyBottom.class)
    // :: error: contracts.postcondition.not.satisfied
    void method(Object x) {}

    @SideEffectsOnly({})
    void method1(@SideEffectsOnlyToyBottom Object x) {}

    void method2(@SideEffectsOnlyToyBottom Object x) {}
}
