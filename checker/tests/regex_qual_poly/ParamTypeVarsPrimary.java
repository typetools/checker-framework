// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.experimental.regex_qual_poly.qual.*;

// Type variables and post-as-member-of
@ClassRegexParam("Main")
class List<T> {
    // (T + MAIN) head
    @Var(arg="Main") T head;
    // List<T><<MAIN>>
    @Var(arg="Main") List<T> tail;
}

abstract class Test {
    abstract @Regex(param="Main")   List<@Regex Integer> makeTT();
    abstract @Regex(value=1, param="Main") List<@Regex Integer> makeUT();
    abstract @Regex(param="Main")   List<@Regex(1) Integer> makeTU();
    abstract @Regex(value=1, param="Main") List<@Regex(1) Integer> makeUU();

    abstract void takeT(@Regex Integer x);
    abstract void takeU(@Regex(1) Integer x);

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
