// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;
import org.checkerframework.qualframework.poly.qual.*;

@ClassTaintingParam("Param1")
class A { }

abstract class Test {

    void test() {
        //:: error: (type.invalid)
        @Tainted(param="Param1") Integer i;
        @Tainted(param="Param1") A a;
        //:: error: (type.invalid)
        @Tainted(param="error")  A a2;
        @Tainted(param="Param1", wildcard=Wildcard.EXTENDS) A a3;
    }
}