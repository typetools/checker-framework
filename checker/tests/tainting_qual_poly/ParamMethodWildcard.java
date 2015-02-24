import org.checkerframework.checker.tainting.qual.*;
import org.checkerframework.qualframework.poly.qual.*;

// Test wildcards with method qualifier parameters.
// No corresponding primary test.
@ClassTaintingParam("Main")
class A { }

abstract class Test {
    static void test1(@Tainted(param="Main") A i, @Tainted(param="Main", wildcard=Wildcard.EXTENDS) A j) { }
    @MethodTaintingParam("Main")
    static void test2(@Var(arg="Main", param="Main") A i, @Var(arg="Main", param="Main", wildcard=Wildcard.EXTENDS) A j) { }

    abstract @Tainted(param="Main") A makeTainted();
    abstract @Untainted(param="Main") A makeUntainted();

    abstract void takeTainted(@Tainted(param="Main") A o);
    abstract void takeUntainted(@Untainted(param="Main") A o);

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
