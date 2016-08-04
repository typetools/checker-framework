// Test qualifier parameter combining.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

@ClassRegexParam("Main")
class PcA {
    // PcB<<Main + TAINTED>> x;
    public @Var(arg = "Main", param = "Main2") @Regex(param = "Main2") PcB x;
    // PcB<<Main + UNTAINTED>> y;
    public @Var(arg = "Main", param = "Main2") @Regex(value = 1, param = "Main2") PcB y;
    // PcB<<Main>> z;
    public @Var(arg = "Main", param = "Main2") PcB z;
}

@ClassRegexParam("Main2")
class PcB {}

abstract class ParamCombine {
    abstract @Regex(param = "Main") PcA makeTainted();

    abstract @Regex(value = 1, param = "Main") PcA makeUntainted();

    abstract void takeTainted(@Regex(param = "Main2") PcB o);

    abstract void takeUntainted(@Regex(value = 1, param = "Main2") PcB o);

    void test() {
        @Regex(param = "Main") PcA ta = makeTainted();
        @Regex(value = 1, param = "Main") PcA ua = makeUntainted();

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
        // The combining rule for Tainting is LUPcB, so the type of ta.y is
        // PcB<<TAINTED + UNTAINTED>> = PcB<<TAINTED>>.
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
