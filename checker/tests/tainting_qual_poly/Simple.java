import org.checkerframework.checker.experimental.tainting_qual_poly.qual.*;

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

// Concatenation doesn't apply to qual params.

//    void concatenation(@Untainted String s1, String s2) {
//        execute(s1 + s1);
//        execute(s1 += s1);
//        execute(s1 + "m");
//        //:: error: (argument.type.incompatible)
//        execute(s1 + s2);   // error
//
//        //:: error: (argument.type.incompatible)
//        execute(s2 + s1);   // error
//        //:: error: (argument.type.incompatible)
//        execute(s2 + "m");  // error
//        //:: error: (argument.type.incompatible)
//        execute(s2 + s2);   // error
//
//        tainted(s1 + s1);
//        tainted(s1 + "m");
//        tainted(s1 + s2);
//
//        tainted(s2 + s1);
//        tainted(s2 + "m");
//        tainted(s2 + s2);
//
//    }
}
