package tests.util.function;

import org.checkerframework.checker.nullness.qual.*;

public interface Function<T extends /*@Nullable*/ Object, R> {
    R apply(T t);
}