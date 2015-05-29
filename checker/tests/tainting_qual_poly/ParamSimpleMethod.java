// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.tainting.qual.*;

// Test method qual parameters
@ClassTaintingParam("Main")
class A { }

abstract class Test {
    @MethodTaintingParam("Main")
    static void test(@Var(arg="Main", param="Main") A i,
            @Var(arg="Main", param="Main") A j) { }

    @MethodTaintingParam("Main")
    @Var(arg="main", param="Main") A test2(@Var(arg="Main", param="Main") A in, A other) {
        //:: error: (return.type.incompatible)
        return makeTainted();
    }

    abstract @Tainted(param="Main") A makeTainted();
    abstract @Untainted(param="Main") A makeUntainted();

    abstract void takeTainted(@Tainted(param="Main") A o);
    abstract void takeUntainted(@Untainted(param="Main") A o);

    void test() {
        test(makeTainted(), makeTainted());
        //:: error: (argument.type.incompatible)
        test(makeTainted(), makeUntainted());
        test(makeUntainted(), makeUntainted());
    }
}
