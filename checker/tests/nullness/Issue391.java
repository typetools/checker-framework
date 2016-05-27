// Test case for Issue 391:
// https://github.com/typetools/checker-framework/issues/391

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;


class ClassA {
    private @Nullable String value = null;

    @EnsuresNonNull("value")
    public void ensuresNonNull() {
        value = "";
    }

    @RequiresNonNull("value")
    public String getValue() {
        return value;
    }
}

public class Issue391 {
    ClassA field = new ClassA();

    @RequiresNonNull("field.value")
    void method() {
    }

    @EnsuresNonNull("field.value")
    void ensuresNonNull() {
        field.ensuresNonNull();
    }

    void method2() {
        ClassA a = new ClassA();
        //:: error: (contracts.precondition.not.satisfied)
        a.getValue();
        //:: error: (contracts.precondition.not.satisfied)
        method();
    }

    void method3() {
        ensuresNonNull();
        method();

        ClassA a = new ClassA();
        a.ensuresNonNull();
        a.getValue();
    }
}
