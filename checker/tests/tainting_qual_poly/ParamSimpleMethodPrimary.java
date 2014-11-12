// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Test method qual params with primary variable
abstract class Test {
    @MethodTaintingParam
    abstract @Var Integer test(@Var Integer i, @Var Integer j);

    @MethodTaintingParam
    @Var Integer test2(@Var Integer in, Integer other) {
        //:: error: (return.type.incompatible)
        return makeTainted();
    }

    abstract @Tainted Integer makeTainted();
    abstract @Untainted Integer makeUntainted();

    abstract void takeTainted(@Tainted Integer o);
    abstract void takeUntainted(@Untainted Integer o);

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
