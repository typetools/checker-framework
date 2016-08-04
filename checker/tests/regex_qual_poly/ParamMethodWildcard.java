import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;
// Test wildcards with method qualifier parameters.
// No corresponding primary test.
@ClassRegexParam("Main")
class PmwA {}

abstract class ParamMethodWildcard {
    static void test1(
            @Regex(param = "Main") PmwA i,
            @Regex(param = "Main", wildcard = Wildcard.EXTENDS) PmwA j) {}

    @MethodRegexParam("Main")
    static void test2(
            @Var(arg = "Main", param = "Main") PmwA i,
            @Var(arg = "Main", param = "Main", wildcard = Wildcard.EXTENDS) PmwA j) {}

    abstract @Regex(param = "Main") PmwA makeTainted();

    abstract @Regex(value = 1, param = "Main") PmwA makeUntainted();

    abstract void takeTainted(@Regex(param = "Main") PmwA o);

    abstract void takeUntainted(@Regex(value = 1, param = "Main") PmwA o);

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
