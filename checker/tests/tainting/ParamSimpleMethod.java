// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.tainting.qual.*;

abstract class Test {
    @TaintingParam("Main")
    static void test(@UseMain Integer i, @UseMain Integer j) { }

    abstract @Tainted Integer makeTainted();
    abstract @Untainted Integer makeUntainted();

    abstract void takeTainted(@Tainted Integer o);
    abstract void takeUntainted(@Untainted Integer o);

    void test() {
        test(makeTainted(), makeTainted());
        //:: error: (argument.type.incompatible)
        test(makeTainted(), makeUntainted());
        test(makeUntainted(), makeUntainted());
    }
}
