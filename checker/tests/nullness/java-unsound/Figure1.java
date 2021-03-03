// Unsound only in Java 8, Java 9+ already gives an error
// @below-java9-jdk-skip-test
// @skip-test noneed to test for the javac error.

public class Figure1 {
    static class Constrain<A, B extends A> {}

    static class Bind<A> {
        <B extends A> A upcast(Constrain<A, B> constrain, B b) {
            return b;
        }
    }

    static <T, U> U coerce(T t) {
        Constrain<U, ? super T> constrain = null;
        Bind<U> bind = new Bind<U>();
        // :: error: method upcast in class Figure1.Bind<A> cannot be applied to given types;
        return bind.upcast(constrain, t);
    }

    public static void main(String[] args) {
        String zero = Figure1.<Integer, String>coerce(0);
    }
}
