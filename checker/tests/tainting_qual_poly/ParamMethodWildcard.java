import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Test wildcards with method qualifier parameters.
// No corresponding primary test.
@TaintingParam("Main")
class A { }

abstract class Test {
    static void test1(@Tainted(target="Main") A i, @Extends(target="Main") @Tainted(target="Main") A j) { }
    @TaintingParam("Main")
    static void test2(@Var(value="Main", target="Main") A i, @Extends(target="Main") @Var(value="Main", target="Main") A j) { }

    abstract @Tainted(target="Main") A makeTainted();
    abstract @Untainted(target="Main") A makeUntainted();

    abstract void takeTainted(@Tainted(target="Main") A o);
    abstract void takeUntainted(@Untainted(target="Main") A o);

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
