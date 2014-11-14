import org.checkerframework.checker.experimental.regex_qual_poly.qual.*;
import org.checkerframework.qualframework.poly.qual.*;

// Test wildcards with method qualifier parameters.
// No corresponding primary test.
@ClassRegexParam("Main")
class A { }

abstract class Test {
    static void test1(@Regex(param="Main") A i, @Regex(param="Main", wildcard=Wildcard.EXTENDS) A j) { }
    @MethodRegexParam("Main")
    static void test2(@Var(arg="Main", param="Main") A i, @Var(arg="Main", param="Main", wildcard=Wildcard.EXTENDS) A j) { }

    abstract @Regex(param="Main") A makeTainted();
    abstract @Regex(value=1, param="Main") A makeUntainted();

    abstract void takeTainted(@Regex(param="Main") A o);
    abstract void takeUntainted(@Regex(value=1, param="Main") A o);

    void test() {
        test1(makeTainted(), makeTainted());
        test1(makeTainted(), makeUntainted());
        //:: error: (argument.type.incompatible)
        test1(makeUntainted(), makeTainted());
        //:: error: (argument.type.incompatible)
        test1(makeUntainted(), makeUntainted());

        test2(makeTainted(), makeTainted());
        test2(makeTainted(), makeUntainted());
        //:: error: (argument.type.incompatible)
        test2(makeUntainted(), makeTainted());
        test2(makeUntainted(), makeUntainted());
    }
}
