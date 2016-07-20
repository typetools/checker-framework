import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

@ClassRegexParam("param1")
class SuperSimple {}

class Simple {

    void takeUntainted(@Regex(value = 1, param = "param1") SuperSimple a) {}

    void takeTainted(@Regex(param = "param1") SuperSimple a) {}

    void takeDef(SuperSimple a) {}

    void test(
            @Regex(value = 1, param = "param1") SuperSimple u,
            @Regex(param = "param1") SuperSimple t,
            SuperSimple def) {

        takeUntainted(u);
        //:: error: (argument.type.incompatible)
        takeTainted(u);
        takeDef(u);

        //:: error: (argument.type.incompatible)
        takeUntainted(t);
        takeTainted(t);
        takeDef(u);

        //:: error: (argument.type.incompatible)
        takeUntainted(def);
        //:: error: (argument.type.incompatible)
        takeTainted(def);
        takeDef(u);
    }

    // Concatenation doesn't apply to qual params, only primary qualifiers
}
