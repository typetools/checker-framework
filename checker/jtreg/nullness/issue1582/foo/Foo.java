package foo;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

public class Foo {

    Foo(@Nullable Object theObject) {}

    @SuppressWarnings("contracts.conditional.postcondition.not.satisfied")
    @EnsuresNonNullIf(
            expression = {"theObject", "getTheObject()"},
            result = true)
    public boolean hasTheObject() {
        return false;
    }

    @Pure
    public @Nullable Object getTheObject() {
        return null;
    }
}
