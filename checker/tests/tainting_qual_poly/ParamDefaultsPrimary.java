// Test parameter defaulting rules.
import org.checkerframework.checker.tainting.qual.*;


abstract class Test {

    abstract Integer makeDefault();
    abstract @Untainted Integer makeUntainted();

    abstract void takeTainted(@Tainted Integer o);
    abstract void takeUntainted(@Untainted Integer o);

    void test() {

        takeUntainted(makeUntainted());
        //:: error: (argument.type.incompatible)
        takeUntainted(makeDefault());
        takeTainted(makeUntainted());
        takeTainted(makeDefault());
    }
}
