// Simple test for qualifier parameters on classes.
import org.checkerframework.checker.experimental.regex_qual_poly.qual.*;

// Test qual param on a class, targeting the primary
@ClassRegexParam("Main")
class A {
    public @Regex Integer x;
    public @Regex(1) Integer y;
    public @Var(arg="Main") Integer z;
}

abstract class Test {
    abstract @Regex(param="Main") A makeTainted();
    abstract @Regex(value=1, param="Main") A makeUntainted();

    abstract void takeTainted(@Regex Integer o);
    abstract void takeUntainted(@Regex(1) Integer o);

    void test() {
        @Regex(param="Main") A ta = makeTainted();
        @Regex(value=1, param="Main") A ua = makeUntainted();

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
