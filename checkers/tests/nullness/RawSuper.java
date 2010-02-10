import checkers.nullness.quals.*;
import java.util.*;

class RawSuper {

    class A {
        @NonNull Object afield;
        A() {
            super();
            mRA(this);
            //:: (type.incompatible)
            mA(this);
            afield = new Object();
            mRA(this);
            mA(this);
        }
    }
    class B extends A {
        @NonNull Object bfield;
        B() {
            mRA(this);
            mA(this);
            mRB(this);
            //:: (type.incompatible)
            mB(this);
            bfield = new Object();
            mRA(this);
            mA(this);
            mRB(this);
            mB(this);
        }
    }
    class C extends B {
        @NonNull Object cfield;
        C() {
            mRA(this);
            mA(this);
            mRB(this);
            mB(this);
            mRC(this);
            //:: (type.incompatible)
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
