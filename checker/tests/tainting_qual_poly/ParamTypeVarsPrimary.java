// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Type variables and post-as-member-of
@ClassTaintingParam("Main")
class List<T> {
    // (T + MAIN) head
    @Var(value="Main") T head;
    // List<T><<MAIN>>
    @Var(value="Main") List<T> tail;
}

abstract class Test {
    abstract @Tainted(target="Main")   List<@Tainted Integer> makeTT();
    abstract @Untainted(target="Main") List<@Tainted Integer> makeUT();
    abstract @Tainted(target="Main")   List<@Untainted Integer> makeTU();
    abstract @Untainted(target="Main") List<@Untainted Integer> makeUU();

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

        // TODO: Add some tail tests

    }
}
