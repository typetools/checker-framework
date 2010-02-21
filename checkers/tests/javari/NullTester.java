import checkers.javari.quals.*;

public class NullTester {

    <T> T identity(T t) {
        return t;
    }

    void method() {
        @Mutable Object a = null;         // mutable

        // ok
        a = a;
        a = identity(a);

        // errors
        //:: (type.incompatible)
        a = (@ReadOnly Object) a;
        //:: (type.incompatible)
        a = identity((@ReadOnly Object)a);

        // ok
        a = null;
        a = identity(null);

    }

}
