// Test case for Issue 1377.
// https://github.com/typetools/checker-framework/issues/1377
// @below-java8-jdk-skip-test

interface Func1377<P, R> {
    R apply(P p);
}

@SuppressWarnings("") // just check for crashes
interface Issue1377<V> {
    static <U> Issue1377<U> of(Issue1377<U> in) {
        return in;
    }

    <S> Issue1377<S> m1(Func1377<? super V, S> f);

    <T> Issue1377<T> m2(Func1377<V, T> f);
}

@SuppressWarnings("") // just check for crashes
class Crash1377 {
    void foo(Issue1377<Void> p) {
        Issue1377.of(p.m1(in -> p)).m2(empty -> 5);
    }
}
