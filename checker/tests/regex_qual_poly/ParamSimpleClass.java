// Simple test for qualifier parameters on classes.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

// Test qual param on a class
@ClassRegexParam("Main")
class PscA {
    public @Regex(param = "Main2") PscB x;
    public @Regex(value = 1, param = "Main2") PscB y;
    public @Var(arg = "Main", param = "Main2") PscB z;
}

@ClassRegexParam("Main2")
class PscB {}

abstract class ParamSimpleClass {
    abstract @Regex(param = "Main") PscA makeTainted();

    abstract @Regex(value = 1, param = "Main") PscA makeUntainted();

    abstract void takeTainted(@Regex(param = "Main2") PscB o);

    abstract void takeUntainted(@Regex(value = 1, param = "Main2") PscB o);

    void test() {
        @Regex(param = "Main") PscA ta = makeTainted();
        @Regex(value = 1, param = "Main") PscA ua = makeUntainted();

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
