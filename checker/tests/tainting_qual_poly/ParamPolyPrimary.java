// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Polymorphic qualifiers, for primary annotations
abstract class Test {
    abstract void test(@PolyTainting Integer i, @PolyTainting Integer j);
    abstract @PolyTainting Integer id(@PolyTainting Integer i);

    abstract @Tainted Integer makeTainted();
    abstract @Untainted Integer makeUntainted();

    abstract void takeTainted(@Tainted Integer o);
    abstract void takeUntainted(@Untainted Integer o);

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
