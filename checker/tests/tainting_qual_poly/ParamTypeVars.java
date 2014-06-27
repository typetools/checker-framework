// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

@TaintingParam("Main")
class List<T> {
    // (T + MAIN) head
    @Var("Main") T head;
    // List<T><<MAIN>>
    @Var("Main") List<T> tail;
}

abstract class Test {
    abstract @Tainted   List<@Tainted   Integer> makeTT();
    abstract @Untainted List<@Tainted   Integer> makeUT();
    abstract @Tainted   List<@Untainted Integer> makeTU();
    abstract @Untainted List<@Untainted Integer> makeUU();

    abstract void takeT(@Tainted   Integer x);
    abstract void takeU(@Untainted Integer x);

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
