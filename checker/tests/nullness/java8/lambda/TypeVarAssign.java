import org.checkerframework.checker.nullness.qual.*;

// @skip-test We can only handle this after we get better method inference.

interface Fn<T> {
    T func(T t);
}

class TestAssign {
    <M extends @NonNull Object> void foo(Fn<M> f) {}

    void context() {
        foo((@NonNull String s) -> s);
    }
}
