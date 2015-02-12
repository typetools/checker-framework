// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.tainting.qual.*;

// Polymorphic method parameters
@ClassTaintingParam("Main")
class A { }

abstract class Test {
    abstract void test(@PolyTainted(param="Main") A i, @PolyTainted(param="Main") A j);
    abstract @PolyTainted(param="Main") A id(@PolyTainted(param="Main") A i);

    abstract @Tainted(param="Main") A makeTainted();
    abstract @Untainted(param="Main") A makeUntainted();

    abstract void takeTainted(@Tainted(param="Main") A o);
    abstract void takeUntainted(@Untainted(param="Main") A o);

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
