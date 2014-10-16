// Simple test for qualifier parameters on classes.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Test qual param on a class, targeting the primary
@TaintingParam("Main")
class A {
    public @Tainted(target="_NONE_") Integer x;
    public @Untainted(target="_NONE_") Integer y;
    public @Var(value="Main", target="_NONE_") Integer z;
}

abstract class Test {
    abstract @Tainted(target="Main") A makeTainted();
    abstract @Untainted(target="Main") A makeUntainted();

    abstract void takeTainted(@Tainted(target="_NONE_") Integer o);
    abstract void takeUntainted(@Untainted(target="_NONE_") Integer o);

    void test() {
        @Tainted(target="Main") A ta = makeTainted();
        @Untainted(target="Main") A ua = makeUntainted();

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
