// Test case for Issue 263:
// https://github.com/typetools/checker-framework/issues/263

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

    public abstract Inner getInner();
}

public class Issue263 {
    public Issue263(Outer<String> outer) {
        this.outer = outer;
    }

    Outer<String> outer;

    public void context() {
        String s = outer.getInner().get();
    }
}
