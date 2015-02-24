// Simple test for qualifier parameters on classes.
import org.checkerframework.checker.tainting.qual.*;

// Test qual param on a class, targeting the primary
@ClassTaintingParam("Main")
class A {
    public @Tainted Integer x;
    public @Untainted Integer y;
    public @Var(arg="Main") Integer z;
}

abstract class Test {
    abstract @Tainted(param="Main") A makeTainted();
    abstract @Untainted(param="Main") A makeUntainted();

    abstract void takeTainted(@Tainted Integer o);
    abstract void takeUntainted(@Untainted Integer o);

    void test() {
        @Tainted(param="Main") A ta = makeTainted();
        @Untainted(param="Main") A ua = makeUntainted();

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
