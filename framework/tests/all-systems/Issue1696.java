// Test case for Issue 1696:
// https://github.com/typetools/checker-framework/issues/1696

class Issue1696 {
    interface I<T extends I<T, S>, S> {}

    void f(I<?, ?> x) {}
}
