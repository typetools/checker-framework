// @skip-tests  qualifier polymorphism system doesn't do capture conversion yet
// Test that capture conversion occurs.
import org.checkerframework.checker.tainting.qual.*;

abstract class Test {
    abstract void takeOne(@PolyTainted Integer i);
    abstract void takeSame(@PolyTainted Integer i, @PolyTainted Integer j);
    abstract @Wild Integer makeInt();

    void test() {
        @Wild Integer i1 = makeInt();
        @Wild Integer i2 = makeInt();

        // This should succeed, inferring <<_poly = CAP#1>> for the method.
        takeOne(i1);

        // This should fail, because CAP#1 and CAP#2 are incompatible.
        //:: error: (argument.type.incompatible)
        takeSame(i1, i2);

        // This should also fail, because the two arguments are captured to
        // distinct variables, even though they're identical expressions.
        //:: error: (argument.type.incompatible)
        takeSame(i1, i1);
    }
}
