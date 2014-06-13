// Test qualifier parameter combining.
import org.checkerframework.checker.tainting.qual.*;

@TaintingParam("Main")
class A {
    // Integer<<Main + TAINTED>> x;
    public @UseMain @Tainted Integer x;
    // Integer<<Main + UNTAINTED>> y;
    public @UseMain @Untainted Integer y;
    // Integer<<Main>> z;
    public @UseMain Integer z;
}

abstract class Test {
    abstract @Tainted A makeTainted();
    abstract @Untainted A makeUntainted();

    abstract void takeTainted(@Tainted Integer o);
    abstract void takeUntainted(@Untainted Integer o);

    void test() {
        @Tainted A ta = makeTainted();
        @Untainted A ua = makeUntainted();

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
