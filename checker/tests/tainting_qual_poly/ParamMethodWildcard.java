import org.checkerframework.checker.tainting.qual.*;
import org.checkerframework.qualframework.poly.qual.*;

// Test wildcards with method qualifier parameters.
// No corresponding primary test.
@ClassTaintingParam("Main")
class PmwA {}

abstract class ParamMethodWildcard {
    static void test1(
            @Tainted(param = "Main") PmwA i,
            @Tainted(param = "Main", wildcard = Wildcard.EXTENDS) PmwA j) {}

    @MethodTaintingParam("Main")
    static void test2(
            @Var(arg = "Main", param = "Main") PmwA i,
            @Var(arg = "Main", param = "Main", wildcard = Wildcard.EXTENDS) PmwA j) {}

    abstract @Tainted(param = "Main") PmwA makeTainted();

    abstract @Untainted(param = "Main") PmwA makeUntainted();

    abstract void takeTainted(@Tainted(param = "Main") PmwA o);

    abstract void takeUntainted(@Untainted(param = "Main") PmwA o);

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
