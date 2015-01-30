// Test case for Issue 263:
// https://code.google.com/p/checker-framework/issues/detail?id=263
// @skip-test

abstract class Outer<T> {

    public class Inner {
        private T t;

        public Inner(T t) {
            this.t = t;
        }

        T get() { return t; }
    }

    abstract public Inner getInner();
}

class Harness {
    public Harness(Outer<String> outer) {
        this.outer = outer;
    }

    Outer<String> outer;

    public void context(  ) {
        String s = outer.getInner().get();
    }
}