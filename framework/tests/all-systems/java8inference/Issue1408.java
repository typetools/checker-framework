// Test case for Issue 1408.
// https://github.com/typetools/checker-framework/issues/1408
abstract class Issue1408 {
    interface Demo {}

    interface SubDemo extends Demo {}

    abstract <S> S foo(S p1, S p2);

    abstract <T extends Demo> T bar(T p2);

    SubDemo demo(SubDemo p) {
        return foo(bar(p), p);
    }
}
