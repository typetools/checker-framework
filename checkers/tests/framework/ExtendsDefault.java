import checkers.quals.*;
import checkers.util.test.*;

class ExtendsDefault {
    
    @DefaultQualifier(value="Odd", locations={DefaultLocation.UPPER_BOUNDS})
    class MyOddDefault<T> { }
    class MyNonOddDefault<T> { }

    void testNonOdd() {
        //:: (generic.argument.invalid)
        MyOddDefault<String> s1;
        MyNonOddDefault<String> s2;
    }

    void testOdd() {
        MyOddDefault<@Odd String> s1;
        MyNonOddDefault<@Odd String> s2;
    }

}
