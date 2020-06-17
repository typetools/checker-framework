// Test case for Issue 3232:
// https://github.com/typetools/checker-framework/issues/3232

class Issue3232A<B> {
    @SuppressWarnings("unchecked")
    void foo(B... values) {}
}

class Issue3232C extends Issue3232A<Integer> {
    void bar(int value) {
        foo(value);
    }
}
