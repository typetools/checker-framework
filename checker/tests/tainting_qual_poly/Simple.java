import org.checkerframework.checker.tainting.qual.*;

@ClassTaintingParam("param1")
class A { }

class Simple {

    void takeUntainted(@Untainted(param="param1") A a) { }
    void takeTainted(@Tainted(param="param1") A a) { }
    void takeDef(A a) { }

    void test(@Untainted(param="param1") A u, @Tainted(param="param1") A t, A def) {

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

// There were some other tests here but concatenation
// doesn't apply to qual params now, only primary qualifiers.

}
