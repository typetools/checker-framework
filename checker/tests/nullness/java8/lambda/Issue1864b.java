// Test case for Issue 1864:
// https://github.com/typetools/checker-framework/issues/1864

class Repro1864 {
    interface Supplier {
        Object get();
    }

    Supplier foo() {
        Object foo = new Object();
        return () -> foo;
    }
}
