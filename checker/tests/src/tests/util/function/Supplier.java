package tests.util.function;

import org.checkerframework.checker.nullness.qual.*;

public interface Supplier<R extends /*@Nullable*/ Object> {
    R supply();
}