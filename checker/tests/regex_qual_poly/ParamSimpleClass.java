// Simple test for qualifier parameters on classes.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

// Test qual param on a class
@ClassRegexParam("Main")
class A {
    public @Regex(param="Main2") B x;
    public @Regex(value=1, param="Main2") B y;
    public @Var(arg="Main", param="Main2") B z;
}

@ClassRegexParam("Main2")
class B { }

abstract class Test {
    abstract @Regex(param="Main") A makeTainted();
    abstract @Regex(value=1, param="Main") A makeUntainted();

    abstract void takeTainted(@Regex(param="Main2") B o);
    abstract void takeUntainted(@Regex(value=1, param="Main2") B o);

    void test() {
        @Regex(param="Main") A ta = makeTainted();
        @Regex(value=1, param="Main") A ua = makeUntainted();

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
