// Test case for Issue 1398
// https://github.com/typetools/checker-framework/issues/1398
// @below-java8-jdk-skip-test

public class Issue1398 {

    interface Pair<A, B> {}

    interface Triple<A, B, C> {}

    interface Quadruple<A, B, C, D> {}

    interface Box<T> {
        <A, B> Pair<A, B> doTriple(Triple<? super T, A, B> t);

        <A, BA extends Box<A>> BA doPair(Pair<? super T, ? extends A> p, BoxMaker<A, BA> bm);
    }

    class BoxMaker<T, C extends Box<T>> {}

    abstract class Crash7 {
        abstract <T, O> Pair<T, O> bar(Pair<T, O> in);

        void foo(
                Box<String> bs,
                BoxMaker<Number, Box<Number>> bm,
                Pair<String, Number> psn,
                Triple<Number, Object, Number> t) {
            bs.doPair(bar(psn), bm).doTriple(t);
        }
    }
}
