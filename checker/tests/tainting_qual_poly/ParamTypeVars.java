// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.tainting.qual.*;

// Type variables and post-as-member-of
// CANT USE Integer here!
@ClassTaintingParam("Param1")
class MyListPtv<T> {
    // (T + MAIN) head
    @Var(arg = "Param1", param = "Param2") T head;
    // MyListPtv<T><<MAIN>>
    @Var(arg = "Param1", param = "Param1") MyListPtv<T> tail;
}

@ClassTaintingParam("Param2")
class PtvA {}

abstract class ParamTypeVars {
    abstract @Tainted(param = "Param1") MyListPtv<@Tainted(param = "Param2") PtvA> makeTT();

    abstract @Untainted(param = "Param1") MyListPtv<@Tainted(param = "Param2") PtvA> makeUT();

    abstract @Tainted(param = "Param1") MyListPtv<@Untainted(param = "Param2") PtvA> makeTU();

    abstract @Untainted(param = "Param1") MyListPtv<@Untainted(param = "Param2") PtvA> makeUU();

    abstract void takeT(@Tainted(param = "Param2") PtvA x);

    abstract void takeU(@Untainted(param = "Param2") PtvA x);

    void test() {
        takeT(makeTT().head);
        takeT(makeUT().head);
        takeT(makeTU().head);
        //:: error: (argument.type.incompatible)
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
