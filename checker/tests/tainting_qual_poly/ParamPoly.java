// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Polymorphic method parameters
@TaintingParam("Main")
class A { }

abstract class Test {
    abstract void test(@PolyTainting(target="Main") A i, @PolyTainting(target="Main") A j);
    abstract @PolyTainting(target="Main") A id(@PolyTainting(target="Main") A i);

    abstract @Tainted(target="Main") A makeTainted();
    abstract @Untainted(target="Main") A makeUntainted();

    abstract void takeTainted(@Tainted(target="Main") A o);
    abstract void takeUntainted(@Untainted(target="Main") A o);

    void test() {
        test(makeTainted(), makeTainted());
        //:: error: (argument.type.incompatible)
        test(makeTainted(), makeUntainted());
        test(makeUntainted(), makeUntainted());

        takeTainted(id(makeTainted()));
        //:: error: (argument.type.incompatible)
        takeTainted(id(makeUntainted()));
        //:: error: (argument.type.incompatible)
        takeUntainted(id(makeTainted()));
        takeUntainted(id(makeUntainted()));
    }
}
