// Simple test for qualifier parameters on classes.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Test qual param on a class
@TaintingParam("Main")
class A {
    public @Tainted Integer x;
    public @Untainted Integer y;
    public @Var("Main") Integer z;
}

abstract class Test {
    abstract @Tainted A makeTainted();
    abstract @Untainted A makeUntainted();

    abstract void takeTainted(@Tainted Integer o);
    abstract void takeUntainted(@Untainted Integer o);

    void test() {
        @Tainted A ta = makeTainted();
        @Untainted A ua = makeUntainted();

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
