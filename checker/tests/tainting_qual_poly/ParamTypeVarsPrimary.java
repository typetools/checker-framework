// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.tainting.qual.*;

// Type variables and post-as-member-of
@ClassTaintingParam("Main")
class List<T> {
    // (T + MAIN) head
    @Var(arg="Main") T head;
    // List<T><<MAIN>>
    @Var(arg="Main") List<T> tail;
}

abstract class Test {
    abstract @Tainted(param="Main")   List<@Tainted Integer> makeTT();
    abstract @Untainted(param="Main") List<@Tainted Integer> makeUT();
    abstract @Tainted(param="Main")   List<@Untainted Integer> makeTU();
    abstract @Untainted(param="Main") List<@Untainted Integer> makeUU();

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
