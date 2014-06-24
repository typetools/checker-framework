// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

abstract class Test {
    @TaintingParam("Main")
    static void test(@Var("Main") Integer i, @Var("Main") Integer j) { }

    abstract @Tainted Integer makeTainted();
    abstract @Untainted Integer makeUntainted();

    abstract void takeTainted(@Tainted Integer o);
    abstract void takeUntainted(@Untainted Integer o);

    void test() {
        test(makeTainted(), makeTainted());
        //:: error: (argument.type.incompatible)
        test(makeTainted(), makeUntainted());
        test(makeUntainted(), makeUntainted());
    }
}
