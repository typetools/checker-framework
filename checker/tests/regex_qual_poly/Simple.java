import org.checkerframework.checker.experimental.regex_qual_poly.qual.*;

@ClassRegexParam("param1")
class A { }

class Simple {

    void takeUntainted(@Regex(value=1, param="param1") A a) { }
    void takeTainted(@Regex(param="param1") A a) { }
    void takeDef(A a) { }

    void test(@Regex(value=1, param="param1") A u, @Regex(param="param1") A t, A def) {

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
