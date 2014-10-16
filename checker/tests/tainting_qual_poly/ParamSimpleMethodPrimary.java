// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Test method qual params with primary variable
abstract class Test {
    @TaintingParam("_NONE_")
    abstract @Var(value="_NONE_", target="_NONE_") Integer test(@Var(value="_NONE_", target="_NONE_") Integer i, @Var(value="_NONE_", target="_NONE_") Integer j);

    abstract @Tainted(target="_NONE_") Integer makeTainted();
    abstract @Untainted(target="_NONE_") Integer makeUntainted();

    abstract void takeTainted(@Tainted(target="_NONE_") Integer o);
    abstract void takeUntainted(@Untainted(target="_NONE_") Integer o);

    void test() {
        takeTainted(test(makeTainted(), makeTainted()));
        takeTainted(test(makeTainted(), makeUntainted()));
        takeTainted(test(makeUntainted(), makeUntainted()));

        //:: error: (argument.type.incompatible)
        takeUntainted(test(makeTainted(), makeTainted()));
        //:: error: (argument.type.incompatible)
        takeUntainted(test(makeTainted(), makeUntainted()));
        takeUntainted(test(makeUntainted(), makeUntainted()));
    }
}
