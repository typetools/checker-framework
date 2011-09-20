import checkers.nullness.quals.*;
import java.util.*;

// @skip-test
// This test is broken as it uses multiple classes.  Javac halts
// when seeing the first error
class RawSuper {

    class A {
        @NonNull Object afield;
        A() {
            super();
            mRA(this);
            //:: error: (type.incompatible)
            mA(this);
            afield = new Object();
            mRA(this);
            mA(this);
        }
        A(int ignore) {
            this.raw();
            afield = new Object();
        }
        void raw(@Raw A this) { }
        void nonRaw() { }
    }

    class B extends A {
        @NonNull Object bfield;
        B() {
            mRA(this);
            mA(this);
            mRB(this);
            //:: error: (type.incompatible)
            mB(this);
            bfield = new Object();
            mRA(this);
            mA(this);
            mRB(this);
            mB(this);
        }
        // "@Raw" means "@Raw B @NonRaw A", but method overrides take precedence.
        void raw(@Raw B this) {
            //:: error: (type.incompatible)
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
            //:: error: (type.incompatible)
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

    void mA(@NonRaw A a) { }
    void mRA(@Raw A a) { }
    void mB(@NonRaw B b) { }
    void mRB(@Raw B b) { }
    void mC(@NonRaw C c) { }
    void mRC(@Raw C c) { }

}
