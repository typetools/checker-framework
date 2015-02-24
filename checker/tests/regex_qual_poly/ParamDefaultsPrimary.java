// Test parameter defaulting rules.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;


abstract class Test {

    abstract Integer makeDefault();
    abstract @Regex(1) Integer makeUntainted();

    abstract void takeTainted(Integer o);
    abstract void takeUntainted(@Regex(1) Integer o);

    void test() {

        takeUntainted(makeUntainted());
        //:: error: (argument.type.incompatible)
        takeUntainted(makeDefault());
        takeTainted(makeUntainted());
        takeTainted(makeDefault());
    }
}
