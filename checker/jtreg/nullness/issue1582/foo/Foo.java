package foo;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Foo {

    Foo(@Nullable Object theObject) {}

    @SuppressWarnings("contracts.conditional.postcondition.not.satisfied")
    @EnsuresNonNullIf(
            expression = {"theObject", "getTheObject()"},
            result = true)
    public boolean hasTheObject() {
        return false;
    }

    @Nullable public Object getTheObject() {
        return null;
    }
}
