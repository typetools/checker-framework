import org.checkerframework.checker.calledmethods.qual.*;

/** Test for postcondition support via @EnsureCalledMethods. */
public class Postconditions {
    void build(@CalledMethods({"a", "b", "c"}) Postconditions this) {}

    void a() {}

    void b() {}

    void c() {}

    @EnsuresCalledMethods(value = "#1", methods = "b")
    static void callB(Postconditions x) {
        x.b();
    }

    @EnsuresCalledMethods(value = "#1", methods = "b")
    // :: error: contracts.postcondition.not.satisfied
    static void doesNotCallB(Postconditions x) {}

    @EnsuresCalledMethods(
            value = "#1",
            methods = {"b", "c"})
    static void callBAndC(Postconditions x) {
        x.b();
        x.c();
    }

    static void allInOneMethod() {
        Postconditions y = new Postconditions();
        y.a();
        y.b();
        y.c();
        y.build();
    }

    static void invokeCallB() {
        Postconditions y = new Postconditions();
        y.a();
        callB(y);
        y.c();
        y.build();
    }

    static void invokeCallBLast() {
        Postconditions y = new Postconditions();
        y.a();
        y.c();
        callB(y);
        y.build();
    }

    static void invokeCallBAndC() {
        Postconditions y = new Postconditions();
        y.a();
        callBAndC(y);
        y.build();
    }

    static void invokeCallBAndCWrong() {
        Postconditions y = new Postconditions();
        callBAndC(y);
        // :: error: finalizer.invocation.invalid
        y.build();
    }

    @EnsuresCalledMethodsIf(
            expression = "#1",
            methods = {"a", "b", "c"},
            result = true)
    static boolean ensuresABCIfTrue(Postconditions p, boolean b) {
        if (b) {
            p.a();
            p.b();
            p.c();
            return true;
        }
        return false;
    }

    static void testEnsuresCalledMethodsIf(Postconditions p, boolean b) {
        if (ensuresABCIfTrue(p, b)) {
            p.build();
        } else {
            // :: error: finalizer.invocation.invalid
            p.build();
        }
    }

    @EnsuresCalledMethods(value = "#1", methods = "a")
    static void callWithException(Postconditions p) {
        try {
            p.a();
            throw new java.io.IOException();
        } catch (java.io.IOException e) {
        }
    }

    @EnsuresCalledMethods(
            value = {"#1", "#2"},
            methods = "a")
    static void callAOnBoth(Postconditions p1, Postconditions p2) {
        p1.a();
        p2.a();
    }

    @EnsuresCalledMethods(
            value = {"#1", "#2"},
            methods = "a")
    static void callAOnBothCatchNPE(Postconditions p1, Postconditions p2) {
        // postcondition is verified because the checker assumes NullPointerExceptions cannot occur
        try {
            p1.a();
        } catch (NullPointerException e) {
        }
        p2.a();
    }

    @EnsuresCalledMethods(
            value = {"#1", "#2"},
            methods = "a")
    static int callAOnBothFinallyNPE(Postconditions p1, Postconditions p2) {
        // postcondition is verified because the checker assumes NullPointerExceptions cannot occur
        try {
            p1.a();
        } finally {
            p2.a();
            return 0;
        }
    }
}
