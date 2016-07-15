// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.tainting.qual.*;

// Polymorphic method parameters
@ClassTaintingParam("Main")
class PpA {}

abstract class ParamPoly {
    abstract void test(@PolyTainted(param = "Main") PpA i, @PolyTainted(param = "Main") PpA j);

    abstract @PolyTainted(param = "Main") PpA id(@PolyTainted(param = "Main") PpA i);

    abstract @Tainted(param = "Main") PpA makeTainted();

    abstract @Untainted(param = "Main") PpA makeUntainted();

    abstract void takeTainted(@Tainted(param = "Main") PpA o);

    abstract void takeUntainted(@Untainted(param = "Main") PpA o);

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
