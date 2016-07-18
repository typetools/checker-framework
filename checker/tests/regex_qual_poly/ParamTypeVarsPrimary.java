// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

// Type variables and post-as-member-of
@ClassRegexParam("Main")
class PTVPList<T> {
    // (T + MAIN) head
    @Var(arg = "Main") T head;
    // PTVPList<T><<MAIN>>
    @Var(arg = "Main") PTVPList<T> tail;
}

abstract class ParamTypeVarsPrimary {
    abstract @Regex(param = "Main") PTVPList<@Regex Integer> makeTT();

    abstract @Regex(value = 1, param = "Main") PTVPList<@Regex Integer> makeUT();

    abstract @Regex(param = "Main") PTVPList<@Regex(1) Integer> makeTU();

    abstract @Regex(value = 1, param = "Main") PTVPList<@Regex(1) Integer> makeUU();

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
