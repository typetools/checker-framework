// Simple test for qualifier parameters on classes.
import org.checkerframework.checker.tainting.qual.*;

// Test qual param on a class
@ClassTaintingParam("Main")
class PscA {
    public @Tainted(param = "Main2") PscB x;
    public @Untainted(param = "Main2") PscB y;
    public @Var(arg = "Main", param = "Main2") PscB z;
}

@ClassTaintingParam("Main2")
class PscB {}

abstract class ParamSimpleClass {
    abstract @Tainted(param = "Main") PscA makeTainted();

    abstract @Untainted(param = "Main") PscA makeUntainted();

    abstract void takeTainted(@Tainted(param = "Main2") PscB o);

    abstract void takeUntainted(@Untainted(param = "Main2") PscB o);

    void test() {
        @Tainted(param = "Main") PscA ta = makeTainted();
        @Untainted(param = "Main") PscA ua = makeUntainted();

        takeTainted(ta.x);
        //:: error: (argument.type.incompatible)
        takeTainted(ta.y);
        takeTainted(ta.z);

        takeTainted(ua.x);
        //:: error: (argument.type.incompatible)
        takeTainted(ua.y);
        //:: error: (argument.type.incompatible)
        takeTainted(ua.z);

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
