// Unsound only in Java 8, Java 9+ already gives an error
// @below-java9-jdk-skip-test
// @skip-test no need to test for the javac error.

public class Figure4 {
    static class Constrain<A, B extends A> {}

    static <A, B extends A> A upcast(Constrain<A, B> constrain, B b) {
        return b;
    }

    static <T, U> U coerce(T t) {
        Constrain<U, ? super T> constrain = null;
        // :: error: method upcast in class Figure4 cannot be applied to given types;
        return upcast(constrain, t);
    }

    public static void main(String[] args) {
        String zero = coerce(0);
    }
}
