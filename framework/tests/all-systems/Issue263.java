// Test case for Issue 263:
// https://github.com/typetools/checker-framework/issues/263

// Suppression required because of Issue 724
// https://github.com/typetools/checker-framework/issues/724
@SuppressWarnings({"regex", "tainting"})
abstract class Outer<T> {

    public class Inner {
        private T t;

        public Inner(T t) {
            this.t = t;
        }

        T get() {
            return t;
        }
    }

    abstract public Inner getInner();
}

class Harness {
    public Harness(Outer<String> outer) {
        this.outer = outer;
    }

    Outer<String> outer;

    public void context() {
        String s = outer.getInner().get();
    }
}
