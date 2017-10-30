import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TypeFrames {

    class A {
        @NonNull String a;

        public A() {
            @UnderInitialization A l1 = this;
            // :: error: (assignment.type.incompatible)
            @UnderInitialization(A.class) A l2 = this;
            a = "";
            @UnderInitialization(A.class) A l3 = this;
        }
    }

    interface I {}

    class B extends A implements I {
        @NonNull String b;

        public B() {
            super();
            @UnderInitialization(A.class) A l1 = this;
            // :: error: (assignment.type.incompatible)
            @UnderInitialization(B.class) A l2 = this;
            b = "";
            @UnderInitialization(B.class) A l3 = this;
        }
    }

    // subtyping
    void t1(@UnderInitialization(A.class) B b1, @UnderInitialization(B.class) B b2) {
        @UnderInitialization(A.class) B l1 = b1;
        @UnderInitialization(A.class) B l2 = b2;
        // :: error: (assignment.type.incompatible)
        @UnderInitialization(B.class) B l3 = b1;
    }
}
