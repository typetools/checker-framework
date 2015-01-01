// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Type variables and post-as-member-of
// CANT USE Integer here!
@ClassTaintingParam("Param1")
class List<T> {
    // (T + MAIN) head
    @Var(arg="Param1", param="Param2") T head;
    // List<T><<MAIN>>
    @Var(arg="Param1", param="Param1") List<T> tail;
}

@ClassTaintingParam("Param2")
class A { }

abstract class Test {
    abstract @Tainted(param="Param1")   List<@Tainted(param="Param2")   A> makeTT();
    abstract @Untainted(param="Param1") List<@Tainted(param="Param2")   A> makeUT();
    abstract @Tainted(param="Param1")   List<@Untainted(param="Param2") A> makeTU();
    abstract @Untainted(param="Param1") List<@Untainted(param="Param2") A> makeUU();

    abstract void takeT(@Tainted(param="Param2")   A x);
    abstract void takeU(@Untainted(param="Param2") A x);

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
