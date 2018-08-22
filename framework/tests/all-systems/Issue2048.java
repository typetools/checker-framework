// Test case for Issue #2048:
// https://github.com/typetools/checker-framework/issues/2048
//
// There are two versions:
// framework/tests/all-systems
// checker/tests/nullness

class Issue2048 {
    interface Foo {}

    interface Fooer<R extends Foo> {}

    class Use<T> {
        @SuppressWarnings("") // Check for crashes.
        void foo(Fooer<? extends T> fooer) {}
    }
}
