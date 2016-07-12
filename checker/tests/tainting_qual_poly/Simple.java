import org.checkerframework.checker.tainting.qual.*;

@ClassTaintingParam("param1")
class SimpleA {}

class Simple {

    void takeUntainted(@Untainted(param = "param1") SimpleA a) {}

    void takeTainted(@Tainted(param = "param1") SimpleA a) {}

    void takeDef(SimpleA a) {}

    void test(
            @Untainted(param = "param1") SimpleA u,
            @Tainted(param = "param1") SimpleA t,
            SimpleA def) {

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
