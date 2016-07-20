// Test parameter defaulting rules.
import org.checkerframework.checker.tainting.qual.*;

@ClassTaintingParam("Main")
class PdA {
    public @Var(arg = "Main", param = "Main2") PdB z;
}

@ClassTaintingParam("Main2")
class PdB {}

abstract class ParamDefaults {
    // Defaults to PdA<<?>> (? is the top of the containment hierarchy, so PdA<<?>>
    // is the top of the hierarchy of instantiations of PdA).
    abstract PdA make();

    abstract @Tainted(param = "Main") PdA makeTainted();

    abstract void takeTainted(@Tainted(param = "Main2") PdB o);

    abstract void takeUntainted(@Untainted(param = "Main2") PdB o);

    abstract void take(PdB o);

    abstract void takePdA(PdA a);

    void test() {
        PdA a = make();
        @Tainted(param = "Main") PdA ta = makeTainted();

        //:: error: (argument.type.incompatible)
        takeUntainted(a.z);
        //:: error: (argument.type.incompatible)
        takeTainted(a.z);
        take(a.z);

        takePdA(a);
        takePdA(ta);
    }
}
