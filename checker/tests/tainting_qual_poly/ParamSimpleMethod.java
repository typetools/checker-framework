// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Test method qual parameters
@ClassTaintingParam("Main")
class A { }

abstract class Test {
    @MethodTaintingParam("Main")
    static void test(@Var(value="Main", target="Main") A i,
            @Var(value="Main", target="Main") A j) { }

    abstract @Tainted(target="Main") A makeTainted();
    abstract @Untainted(target="Main") A makeUntainted();

    abstract void takeTainted(@Tainted(target="Main") A o);
    abstract void takeUntainted(@Untainted(target="Main") A o);

    void test() {
        test(makeTainted(), makeTainted());
        //:: error: (argument.type.incompatible)
        test(makeTainted(), makeUntainted());
        test(makeUntainted(), makeUntainted());
    }
}
