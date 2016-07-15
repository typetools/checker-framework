// Test parameter defaulting rules.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

@ClassRegexParam("Main")
class PdA {
    public @Var(arg = "Main", param = "Main2") B z;
}

@ClassRegexParam("Main2")
class B {}

abstract class ParamDefaults {
    // Defaults to PdA<<?>> (? is the top of the containment hierarchy, so PdA<<?>>
    // is the top of the hierarchy of instantiations of PdA).
    abstract PdA make();

    abstract @Regex(param = "Main") PdA makeTainted();

    abstract void takeTainted(@Regex(param = "Main2") B o);

    abstract void takeUntainted(@Regex(value = 1, param = "Main2") B o);

    abstract void take(B o);

    abstract void takePdA(PdA a);

    void test() {
        PdA a = make();
        @Regex(param = "Main") PdA ta = makeTainted();

        //:: error: (argument.type.incompatible)
        takeUntainted(a.z);
        //:: error: (argument.type.incompatible)
        takeTainted(a.z);
        take(a.z);

        takePdA(a);
        takePdA(ta);
    }
}
