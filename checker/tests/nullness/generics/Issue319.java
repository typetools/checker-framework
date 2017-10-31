// Test case for Issue 319:
// https://github.com/typetools/checker-framework/issues/319

import org.checkerframework.checker.nullness.qual.*;

class Issue319 {
    class Foo<T> {
        Foo(@Nullable T t) {}
    }

    <T> Foo<T> newFoo(@Nullable T t) {
        return new Foo<T>(t);
    }

    void pass() {
        Foo<Boolean> f = newFoo(Boolean.FALSE);
    }

    void fail() {
        Foo<Boolean> f = newFoo(null);
    }

    void workaround() {
        Foo<Boolean> f = Issue319.this.<Boolean>newFoo(null);
    }
}

class Issue319NN {
    class Foo<T> {
        Foo(@NonNull T t) {}
    }

    <T> Foo<T> newFoo(@NonNull T t) {
        return new Foo<T>(t);
    }

    void pass() {
        Foo<Boolean> f = newFoo(Boolean.FALSE);
    }

    void fail() {
        // :: error: (argument.type.incompatible)
        Foo<Boolean> f = newFoo(null);
    }

    void pass2() {
        Foo<@Nullable Boolean> f = newFoo(Boolean.FALSE);
    }
}
