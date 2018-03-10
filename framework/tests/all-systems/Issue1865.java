// Test case for Issue 1865:
// https://github.com/typetools/checker-framework/issues/1865

abstract class Issue1865 {

    // Widening conversion

    abstract int f();

    abstract int max(int... array);

    void g() {
        long l = max(f(), f());
    }

    // String conversion

    abstract Object h(Object... args);

    void i() {
        Object o = "" + h();
    }
}
