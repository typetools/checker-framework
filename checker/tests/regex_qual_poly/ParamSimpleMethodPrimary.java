// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

// Test method qual params with primary variable
abstract class Test {
    @MethodRegexParam
    abstract @Var Integer test(@Var Integer i, @Var Integer j);

    @MethodRegexParam
    @Var Integer test2(@Var Integer in, Integer other) {
        //:: error: (return.type.incompatible)
        return makeTainted();
    }

    abstract @Regex Integer makeTainted();
    abstract @Regex(1) Integer makeUntainted();

    abstract void takeTainted(@Regex Integer o);
    abstract void takeUntainted(@Regex(1) Integer o);

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
