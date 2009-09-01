import checkers.javari.quals.*;

public class NullTester {

    <T> T identity(T t) {
        return t;
    }

    void method() {
        Object a = null;         // mutable

        // ok
        a = a;
        a = identity(a);

        // errors
        a = (@ReadOnly Object) a;
        a = identity((@ReadOnly Object)a);

        // ok
        a = null;
        a = identity(null);

    }

}
