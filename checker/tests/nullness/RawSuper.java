import org.checkerframework.checker.nullness.qual.*;

// @skip-test
// This test is broken as it uses multiple classes.  Javac halts
// when seeing the first error
public class RawSuper {

    class A {
        @NonNull Object afield;

        A() {
            super();
            mRA(this);
            // :: error: (type.incompatible)
            mA(this);
            afield = new Object();
            mRA(this);
            mA(this);
        }

        A(int ignore) {
            this.raw();
            afield = new Object();
        }

        void raw(A this) {}

        void nonRaw() {}
    }

    class B extends A {
        @NonNull Object bfield;

        B() {
            mRA(this);
            mA(this);
            mRB(this);
            // :: error: (type.incompatible)
            mB(this);
            bfield = new Object();
            mRA(this);
            mA(this);
            mRB(this);
            mB(this);
        }

        void raw(B this) {
            // :: error: (type.incompatible)
            super.nonRaw();
        }
    }
    // This test may be extraneous
    class C extends B {
        @NonNull Object cfield;

        C() {
            mRA(this);
            mA(this);
            mRB(this);
            mB(this);
            mRC(this);
            // :: error: (type.incompatible)
            mC(this);
            cfield = new Object();
            mRA(this);
            mA(this);
            mRB(this);
            mB(this);
            mRC(this);
            mC(this);
        }
    }

    void mA(A a) {}

    void mRA(A a) {}

    void mB(B b) {}

    void mRB(B b) {}

    void mC(C c) {}

    void mRC(C c) {}
}
