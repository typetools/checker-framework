// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.experimental.regex_qual_poly.qual.*;

// Type variables and post-as-member-of
@ClassRegexParam("Param1")
class List<T> {
    // (T + MAIN) head
    @Var(arg="Param1", param="Param2") T head;
    // List<T><<MAIN>>
    @Var(arg="Param1", param="Param1") List<T> tail;
}

@ClassRegexParam("Param2")
class A { }

abstract class Test {
    abstract @Regex(param="Param1")   List<@Regex(param="Param2")   A> makeTT();
    abstract @Regex(value=1, param="Param1") List<@Regex(param="Param2")   A> makeUT();
    abstract @Regex(param="Param1")   List<@Regex(value=1, param="Param2") A> makeTU();
    abstract @Regex(value=1, param="Param1") List<@Regex(value=1, param="Param2") A> makeUU();

    abstract void takeT(@Regex(param="Param2")   A x);
    abstract void takeU(@Regex(value=1, param="Param2") A x);

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
