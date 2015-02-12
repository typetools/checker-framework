// Test qualifier parameter combining.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

@ClassRegexParam("Main")
class A {
    // B<<Main + TAINTED>> x;
    public @Var(arg="Main", param="Main2") @Regex(param="Main2") B x;
    // B<<Main + UNTAINTED>> y;
    public @Var(arg="Main", param="Main2") @Regex(value=1, param="Main2") B y;
    // B<<Main>> z;
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
        takeTainted(ta.y);
        takeTainted(ta.z);
        takeTainted(ua.x);
        //:: error: (argument.type.incompatible)
        takeTainted(ua.y);
        //:: error: (argument.type.incompatible)
        takeTainted(ua.z);

        //:: error: (argument.type.incompatible)
        takeUntainted(ta.x);
        // The combining rule for Tainting is LUB, so the type of ta.y is
        // B<<TAINTED + UNTAINTED>> = B<<TAINTED>>.
        //:: error: (argument.type.incompatible)
        takeUntainted(ta.y);
        //:: error: (argument.type.incompatible)
        takeUntainted(ta.z);

        //:: error: (argument.type.incompatible)
        takeUntainted(ua.x);
        takeUntainted(ua.y);
        takeUntainted(ua.z);
    }
}
