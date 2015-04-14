// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

@ClassRegexParam("Param1")
class A { }

abstract class Test {

    void test() {
        //:: error: (type.invalid)
        @Regex(param="Param1") Integer i;
        @Regex(param="Param1") A a;
        //:: error: (type.invalid)
        @Regex(param="error")  A a2;
        @Regex(param="Param1", wildcard=Wildcard.EXTENDS) A a3;
    }
}