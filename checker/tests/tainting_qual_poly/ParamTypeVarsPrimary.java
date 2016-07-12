// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.tainting.qual.*;

// Type variables and post-as-member-of
@ClassTaintingParam("Main")
class PtvpList<T> {
    // (T + MAIN) head
    @Var(arg = "Main") T head;
    // PtvpList<T><<MAIN>>
    @Var(arg = "Main") PtvpList<T> tail;
}

abstract class ParamTypeVarsPrimar {
    abstract @Tainted(param = "Main") PtvpList<@Tainted Integer> makeTT();

    abstract @Untainted(param = "Main") PtvpList<@Tainted Integer> makeUT();

    abstract @Tainted(param = "Main") PtvpList<@Untainted Integer> makeTU();

    abstract @Untainted(param = "Main") PtvpList<@Untainted Integer> makeUU();

    abstract void takeT(@Tainted Integer x);

    abstract void takeU(@Untainted Integer x);

    void test() {
        takeT(makeTT().head);
        takeT(makeUT().head);
        takeT(makeTU().head);
        takeT(makeUU().head);

        //:: error: (argument.type.incompatible)
        takeU(makeTT().head);
        //:: error: (argument.type.incompatible)
        takeU(makeUT().head);
        //:: error: (argument.type.incompatible)
        takeU(makeTU().head);
        takeU(makeUU().head);
    }
}
