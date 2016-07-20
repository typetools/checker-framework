// Test qualifier parameter + type parameter combining.
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

// Type variables and post-as-member-of
@ClassRegexParam("Param1")
class ListLinked<T> {
    // (T + MAIN) head
    @Var(arg = "Param1", param = "Param2") T head;
    // ListLinked<T><<MAIN>>
    @Var(arg = "Param1", param = "Param1") ListLinked<T> tail;
}

@ClassRegexParam("Param2")
class TypeVarsA {}

abstract class ParamTypeVars {
    abstract @Regex(param = "Param1") ListLinked<@Regex(param = "Param2") TypeVarsA> makeTT();

    abstract @Regex(value = 1, param = "Param1") ListLinked<@Regex(param = "Param2") TypeVarsA>
            makeUT();

    abstract @Regex(param = "Param1") ListLinked<@Regex(value = 1, param = "Param2") TypeVarsA>
            makeTU();

    abstract @Regex(value = 1, param = "Param1") ListLinked<
                    @Regex(value = 1, param = "Param2") TypeVarsA>
            makeUU();

    abstract void takeT(@Regex(param = "Param2") TypeVarsA x);

    abstract void takeU(@Regex(value = 1, param = "Param2") TypeVarsA x);

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
