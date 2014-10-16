// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Polymorphic qualifiers, for primary annotations
abstract class Test {
    abstract void test(@PolyTainting(target="_NONE_") Integer i, @PolyTainting(target="_NONE_") Integer j);
    abstract @PolyTainting(target="_NONE_") Integer id(@PolyTainting(target="_NONE_") Integer i);

    abstract @Tainted(target="_NONE_") Integer makeTainted();
    abstract @Untainted(target="_NONE_") Integer makeUntainted();

    abstract void takeTainted(@Tainted(target="_NONE_") Integer o);
    abstract void takeUntainted(@Untainted(target="_NONE_") Integer o);

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
