// Unsound only in Java 8, Java 9+ already gives an error
// @below-java9-jdk-skip-test
// @skip-test noneed to test for the javac error.

public class Figure7<T, U> {
    class Constrain<B extends U> {}

    final Constrain<? super T> constrain;
    final U u;

    Figure7(T t) {
        u = coerce(t);
        constrain = getConstrain();
    }

    <B extends U> U upcast(Constrain<B> constrain, B b) {
        return b;
    }

    U coerce(T t) {
        // :: error: method upcast in class Figure7<T,U> cannot be applied to given types;
        return upcast(constrain, t);
    }

    Constrain<? super T> getConstrain() {
        return constrain;
    }

    public static void main(String[] args) {
        String zero = new Figure7<Integer, String>(0).u;
    }
}
