import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class RawCheckRep {

    Object x;

    RawCheckRep() {
        x = "hello";
        checkRep();
        checkRep2(this);
        checkRepb();
        checkRep2b(this);
    }

    void checkRep(
            @UnderInitialization(RawCheckRep.class) @Raw(RawCheckRep.class) RawCheckRep this) {
        x.toString();
    }

    static void checkRep2(
            @UnderInitialization(RawCheckRep.class) @Raw(RawCheckRep.class) RawCheckRep o) {
        o.x.toString();
    }

    void checkRepb(@UnderInitialization(Object.class) @Raw(Object.class) RawCheckRep this) {
        // :: error: (dereference.of.nullable)
        x.toString();
    }

    static void checkRep2b(@UnderInitialization(Object.class) @Raw(Object.class) RawCheckRep o) {
        // :: error: (dereference.of.nullable)
        o.x.toString();
    }
}

class A {
    String a;

    public A() {
        a = "";
    }
}

class B extends A {
    String b;

    public B() {
        b = "";
    }

    void t1(@UnderInitialization(Object.class) @Raw(Object.class) B x) {
        // :: error: (dereference.of.nullable)
        x.a.toString();
        // :: error: (dereference.of.nullable)
        x.b.toString();
    }

    void t2(@UnderInitialization(A.class) @Raw(A.class) B x) {
        x.a.toString();
        // :: error: (dereference.of.nullable)
        x.b.toString();
    }

    void t3(@UnderInitialization(B.class) @Raw(B.class) B x) {
        x.a.toString();
        x.b.toString();
    }
}
