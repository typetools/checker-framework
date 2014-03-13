// Test case for Issue 139:
// http://code.google.com/p/checker-framework/issues/detail?id=139
abstract class GenericTest8 {
    interface A<S> {}

    void foo1(A<?> a) {
        foo2(a);
    }

    abstract <T> A<? extends T> foo2(A<? extends T> a);

    void bar1(A<? extends A<?>> a) {
        bar2(a);
    }

    abstract <U> A<A<? extends U>> bar2(A<? extends A<? extends U>> a);
}
