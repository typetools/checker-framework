import checkers.quals.*;
import tests.util.*;

class ExtendsDefault {

    @DefaultQualifier(value="Odd", locations={DefaultLocation.UPPER_BOUNDS})
    class MyOddDefault<T> { }
    class MyNonOddDefault<T> { }

    void testNonOdd() {
        //:: error: (type.argument.type.incompatible)
        MyOddDefault<String> s1;
        MyNonOddDefault<String> s2;
    }

    void testOdd() {
        MyOddDefault<@Odd String> s1;
        MyNonOddDefault<@Odd String> s2;
    }

}
