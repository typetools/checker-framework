// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.tainting.qual.*;
import org.checkerframework.qualframework.poly.qual.*;

@ClassTaintingParam("Param1")
class EmA {}

abstract class ErrorMessages {

    void test() {
        //:: error: (type.invalid)
        @Tainted(param = "Param1") Integer i;
        @Tainted(param = "Param1") EmA a;
        //:: error: (type.invalid)
        @Tainted(param = "error") EmA a2;
        @Tainted(param = "Param1", wildcard = Wildcard.EXTENDS) EmA a3;
    }
}
