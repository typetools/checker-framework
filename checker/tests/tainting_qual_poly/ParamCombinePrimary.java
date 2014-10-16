// Test qualifier parameter combining.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

@TaintingParam("Main")
class A {
    // Integer<<Main + TAINTED>> x;
    public @Var(value="Main", target="_NONE_") @Tainted(target="_NONE_") Integer x;
    // Integer<<Main + UNTAINTED>> y;
    public @Var(value="Main", target="_NONE_") @Untainted(target="_NONE_") Integer y;
    // Integer<<Main>> z;
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
