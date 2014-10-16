// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

// Type variables and post-as-member-of
// CANT USE Integer here!
@TaintingParam("Main")
class List<T> {
    // (T + MAIN) head
    @Var("Main") T head;
    // List<T><<MAIN>>
    @Var("Main") List<T> tail;
}

@TaintingParam("Main")
class A {

}

abstract class Test {
    abstract @Tainted   List<@Tainted   A> makeTT();
    abstract @Untainted List<@Tainted   A> makeUT();
    abstract @Tainted   List<@Untainted A> makeTU();
    abstract @Untainted List<@Untainted A> makeUU();

    abstract void takeT(@Tainted   A x);
    abstract void takeU(@Untainted A x);

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
