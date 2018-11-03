import org.checkerframework.checker.nullness.qual.*;

class BoxingGenerics {
    static class X<T> {
        public static <T> X<T> foo(T x) {
            return new X<>();
        }

        public void bar(X<T> x) {}
    }

    public void getText() {
        X<Integer> var = new X<>();
        X.foo(new Integer(5)).bar(var);
        X.foo(5).bar(var);
    }
}
