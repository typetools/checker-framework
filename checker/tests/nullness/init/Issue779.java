// Test case for Issue #779
// https://github.com/typetools/checker-framework/issues/779

import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class A {
    Object g = new Object();

    A() {
        foo();
    }

    void foo(@Raw(A.class) @UnderInitialization(A.class) A this) {
        System.out.println("foo A " + g.toString());
    }
}

class B extends A {
    Object f = new Object();

    void foo(@Raw(A.class) @UnderInitialization(A.class) B this) {
        // :: error: (dereference.of.nullable)
        System.out.println("foo B " + this.f.toString());
    }
}

public class Issue779 {
    public static void main(String[] args) {
        new B();
    }
}
