// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

@ClassTaintingParam("Param1")
class A { }

abstract class Test {

    void test() {
        //:: error: (type.invalid)
        @Tainted(target="Param1") Integer i;
        @Tainted(target="Param1") A a;
        //:: error: (type.invalid)
        @Tainted(target="error")  A a2;
        @Tainted(target="Param1") @Extends(target="Param1") A a3;

        // TODO: These should give off errors
        @Extends(target="Param1") A a4;
        @Extends(target="error")  A a5;
    }
}