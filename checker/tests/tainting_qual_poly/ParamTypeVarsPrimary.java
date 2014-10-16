// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Type variables and post-as-member-of
@TaintingParam("Main")
class List<T> {
    // (T + MAIN) head
    @Var(value="Main", target="_NONE_") T head;
    // List<T><<MAIN>>
    @Var(value="Main", target="_NONE_") List<T> tail;
}

abstract class Test {
    abstract @Tainted(target="Main")   List<@Tainted(target="_NONE_")   Integer> makeTT();
    abstract @Untainted(target="Main") List<@Tainted(target="_NONE_")   Integer> makeUT();
    abstract @Tainted(target="Main")   List<@Untainted(target="_NONE_") Integer> makeTU();
    abstract @Untainted(target="Main") List<@Untainted(target="_NONE_") Integer> makeUU();

    abstract void takeT(@Tainted(target="_NONE_")   Integer x);
    abstract void takeU(@Untainted(target="_NONE_") Integer x);

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
