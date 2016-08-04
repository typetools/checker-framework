// Simple test for qualifier parameters on classes.
import org.checkerframework.checker.tainting.qual.*;

// Test qual param on a class, targeting the primary
@ClassTaintingParam("Main")
class PscpA {
    public @Tainted Integer x;
    public @Untainted Integer y;
    public @Var(arg = "Main") Integer z;
}

abstract class ParamSimpleClassPrimary {
    abstract @Tainted(param = "Main") PscpA makeTainted();

    abstract @Untainted(param = "Main") PscpA makeUntainted();

    abstract void takeTainted(@Tainted Integer o);

    abstract void takeUntainted(@Untainted Integer o);

    void test() {
        @Tainted(param = "Main") PscpA ta = makeTainted();
        @Untainted(param = "Main") PscpA ua = makeUntainted();

        //:: error: (argument.type.incompatible)
        takeUntainted(ta.x);
        takeUntainted(ta.y);
        //:: error: (argument.type.incompatible)
        takeUntainted(ta.z);

        //:: error: (argument.type.incompatible)
        takeUntainted(ua.x);
        takeUntainted(ua.y);
        takeUntainted(ua.z);
    }
}
