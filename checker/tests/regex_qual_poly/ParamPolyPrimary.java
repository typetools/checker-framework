// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.experimental.regex_qual_poly.qual.*;

// Polymorphic qualifiers, for primary annotations
abstract class Test {
    abstract void test(@PolyRegex Integer i, @PolyRegex Integer j);
    abstract @PolyRegex Integer id(@PolyRegex Integer i);

    abstract @Regex Integer makeTainted();
    abstract @Regex(1) Integer makeUntainted();

    abstract void takeTainted(@Regex Integer o);
    abstract void takeUntainted(@Regex(1) Integer o);

    void test() {
        test(makeTainted(), makeTainted());
        test(makeTainted(), makeUntainted());
        test(makeUntainted(), makeUntainted());

        takeTainted(id(makeTainted()));
        takeTainted(id(makeUntainted()));

        //:: error: (argument.type.incompatible)
        takeUntainted(id(makeTainted()));
        takeUntainted(id(makeUntainted()));
    }
}
