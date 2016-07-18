// Simple test for qualifier parameters on classes.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

// Test qual param on a class, targeting the primary
@ClassRegexParam("Main")
class Pscp {
    public @Regex Integer x;
    public @Regex(1) Integer y;
    public @Var(arg = "Main") Integer z;
}

abstract class ParamSimpleClassPrimary {
    abstract @Regex(param = "Main") Pscp makeTainted();

    abstract @Regex(value = 1, param = "Main") Pscp makeUntainted();

    abstract void takeTainted(@Regex Integer o);

    abstract void takeUntainted(@Regex(1) Integer o);

    void test() {
        @Regex(param = "Main") Pscp ta = makeTainted();
        @Regex(value = 1, param = "Main") Pscp ua = makeUntainted();

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
