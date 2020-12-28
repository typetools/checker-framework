package annotatedforlib;

import org.checkerframework.checker.nullness.qual.Nullable;

public class Test<T> {
    public void method1(T t) {}

    public void method2(@Nullable T t) {}
}
