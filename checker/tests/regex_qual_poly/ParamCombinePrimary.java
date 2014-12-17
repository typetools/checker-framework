// Test qualifier parameter combining.
import org.checkerframework.checker.experimental.regex_qual_poly.qual.*;

@ClassRegexParam("Main")
class A {
    // Integer<<Main + TAINTED>> x;
    public @Var(arg="Main") @Regex Integer x;
    // Integer<<Main + UNTAINTED>> y;
    public @Var(arg="Main") @Regex(1) Integer y;
    // Integer<<Main>> z;
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

        takeTainted(ta.x);
        takeTainted(ta.y);
        takeTainted(ta.z);
        takeTainted(ua.x);
        takeTainted(ua.y);
        takeTainted(ua.z);

        //:: error: (argument.type.incompatible)
        takeUntainted(ta.x);
        // The combining rule for Tainting is LUB, so the type of ta.y is
        // Integer<<TAINTED + UNTAINTED>> = Integer<<TAINTED>>.
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
