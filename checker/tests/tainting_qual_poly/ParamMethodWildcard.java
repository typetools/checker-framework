import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Test wildcards with method qualifier parameters.
// No corresponding primary test.
abstract class Test {
    static void test1(@Tainted Integer i, @Extends @Tainted Integer j) { }
    @TaintingParam("Main")
    static void test2(@Var("Main") Integer i, @Extends @Var("Main") Integer j) { }

    abstract @Tainted Integer makeTainted();
    abstract @Untainted Integer makeUntainted();

    abstract void takeTainted(@Tainted Integer o);
    abstract void takeUntainted(@Untainted Integer o);

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
