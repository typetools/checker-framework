// Test qualifier parameter combining.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

@ClassRegexParam("Main")
class PcpA {
    // Integer<<Main + TPcpAINTED>> x;
    public @Var(arg = "Main") @Regex Integer x;
    // Integer<<Main + UNTPcpAINTED>> y;
    public @Var(arg = "Main") @Regex(1) Integer y;
    // Integer<<Main>> z;
    public @Var(arg = "Main") Integer z;
}

abstract class ParamCombinePrimary {
    abstract @Regex(param = "Main") PcpA makeTainted();

    abstract @Regex(value = 1, param = "Main") PcpA makeUntainted();

    abstract void takeTainted(@Regex Integer o);

    abstract void takeUntainted(@Regex(1) Integer o);

    void test() {
        @Regex(param = "Main") PcpA ta = makeTainted();
        @Regex(value = 1, param = "Main") PcpA ua = makeUntainted();

        takeTainted(ta.x);
        takeTainted(ta.y);
        takeTainted(ta.z);
        takeTainted(ua.x);
        takeTainted(ua.y);
        takeTainted(ua.z);

        //:: error: (argument.type.incompatible)
        takeUntainted(ta.x);
        // The combining rule for Tainting is LUB, so the type of ta.y is
        // Integer<<TPcpAINTED + UNTPcpAINTED>> = Integer<<TPcpAINTED>>.
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
