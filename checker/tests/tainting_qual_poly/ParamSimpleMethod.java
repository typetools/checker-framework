// Simple test for qualifier parameters on methods.
import org.checkerframework.checker.tainting.qual.*;

// Test method qual parameters
@ClassTaintingParam("Main")
class PsmA {}

abstract class ParamSimpleMethod {
    @MethodTaintingParam("Main")
    static void test(
            @Var(arg = "Main", param = "Main") PsmA i, @Var(arg = "Main", param = "Main") PsmA j) {}

    @MethodTaintingParam("Main")
    @Var(arg = "main", param = "Main") PsmA test2(@Var(arg = "Main", param = "Main") PsmA in, PsmA other) {
        //:: error: (return.type.incompatible)
        return makeTainted();
    }

    abstract @Tainted(param = "Main") PsmA makeTainted();

    abstract @Untainted(param = "Main") PsmA makeUntainted();

    abstract void takeTainted(@Tainted(param = "Main") PsmA o);

    abstract void takeUntainted(@Untainted(param = "Main") PsmA o);

    void test() {
        test(makeTainted(), makeTainted());
        //:: error: (argument.type.incompatible)
        test(makeTainted(), makeUntainted());
        test(makeUntainted(), makeUntainted());
    }
}
