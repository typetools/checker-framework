// Test case for Issue 391:
// https://github.com/typetools/checker-framework/issues/391

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

class ClassA {
    @Nullable private String value = null;

    @RequiresNonNull("value") public String getValue() {
        return value;
    }
}

public class Issue391 {
    ClassA field = new ClassA();

    @RequiresNonNull("field.value")
    void method() {
    }

    void method2() {
        ClassA a = new ClassA();
        //:: error: (contracts.precondition.not.satisfied)
        a.getValue();
        //:: error: (contracts.precondition.not.satisfied)
        method();
    }
}
