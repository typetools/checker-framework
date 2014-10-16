// Test qualifier parameter combining.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

@TaintingParam("Main")
class A {
    // Integer<<Main + TAINTED>> x;
    public @Var("Main") @Tainted Integer x;
    // Integer<<Main + UNTAINTED>> y;
    public @Var("Main") @Untainted Integer y;
    // Integer<<Main>> z;
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
