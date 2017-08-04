// Test case for Issue 1417.
// https://github.com/typetools/checker-framework/issues/1417
// @below-java8-jdk-skip-test

class Issue1417 {
    interface Bar {}

    interface SubBar extends Bar {}

    interface Barber<S extends Bar> {
        S call(S s);
    }

    abstract class Crash12 {
        abstract void foo(Barber<?> b);

        void crash() {
            foo((SubBar p) -> p);
        }
    }
}
