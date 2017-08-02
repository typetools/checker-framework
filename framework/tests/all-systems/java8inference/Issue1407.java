// Test case for Issue 1407.
// https://github.com/typetools/checker-framework/issues/1407
// @below-java8-jdk-skip-test

abstract class Issue1407 {
    abstract <T> T foo(T p1, T p2);

    abstract <T extends Number> T bar(int p1, T p2);

    @SuppressWarnings({"interning", "signedness"})
    int demo() {
        return foo(bar(5, 3), 3);
    }
}
