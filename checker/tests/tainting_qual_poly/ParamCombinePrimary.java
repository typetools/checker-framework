// Test qualifier parameter combining.
import org.checkerframework.checker.tainting.qual.*;

@ClassTaintingParam("Main")
class PcpA {
    // Integer<<Main + TAINTED>> x;
    public @Var(arg = "Main") @Tainted Integer x;
    // Integer<<Main + UNTAINTED>> y;
    public @Var(arg = "Main") @Untainted Integer y;
    // Integer<<Main>> z;
    public @Var(arg = "Main") Integer z;
}

abstract class ParamCombinePrimary {
    abstract @Tainted(param = "Main") PcpA makeTainted();

    abstract @Untainted(param = "Main") PcpA makeUntainted();

    abstract void takeTainted(@Tainted Integer o);

    abstract void takeUntainted(@Untainted Integer o);

    void test() {
        @Tainted(param = "Main") PcpA ta = makeTainted();
        @Untainted(param = "Main") PcpA ua = makeUntainted();

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
