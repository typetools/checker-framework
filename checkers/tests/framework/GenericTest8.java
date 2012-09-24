// Test case for Issue 138:
// http://code.google.com/p/checker-framework/issues/detail?id=138
abstract class GenericTest8 {
    interface A<S> {}

    void foo1(A<?> a) {
        // TODO: this call should not fail, I believe. The test case
        // ensures that the compiler doesn't crash;
        // this error is not desired.
        //:: error: (argument.type.incompatible)
        foo2(a);
    }

    abstract <T> A<? extends T> foo2(A<? extends T> a);

    void bar1(A<? extends A<?>> a) {
        bar2(a);
    }

    abstract <U> A<A<? extends U>> bar2(A<? extends A<? extends U>> a);
}
