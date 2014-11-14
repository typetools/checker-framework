// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.experimental.regex_qual_poly.qual.*;

// Test method qual parameters
@ClassRegexParam("Main")
class A { }

abstract class Test {
    @MethodRegexParam("Main")
    static void test(@Var(arg="Main", param="Main") A i,
            @Var(arg="Main", param="Main") A j) { }

    @MethodRegexParam("Main")
    @Var(arg="main", param="Main") A test2(@Var(arg="Main", param="Main") A in, A other) {
        //:: error: (return.type.incompatible)
        return makeTainted();
    }

    abstract @Regex(param="Main") A makeTainted();
    abstract @Regex(value=1, param="Main") A makeUntainted();

    abstract void takeTainted(@Regex(param="Main") A o);
    abstract void takeUntainted(@Regex(value=1, param="Main") A o);

    void test() {
        test(makeTainted(), makeTainted());
        //:: error: (argument.type.incompatible)
        test(makeTainted(), makeUntainted());
        test(makeUntainted(), makeUntainted());
    }
}
