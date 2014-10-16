// Test parameter defaulting rules.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;


abstract class Test {

    abstract Integer makeDefault();
    abstract @Untainted(target="_NONE_") Integer makeUntainted();

    abstract void takeTainted(@Tainted(target="_NONE_") Integer o);
    abstract void takeUntainted(@Untainted(target="_NONE_") Integer o);

    void test() {

        takeUntainted(makeUntainted());
        //:: error: (argument.type.incompatible)
        takeUntainted(makeDefault());
        takeTainted(makeUntainted());
        takeTainted(makeDefault());
    }
}
