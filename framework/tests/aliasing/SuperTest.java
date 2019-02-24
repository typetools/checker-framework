import org.checkerframework.common.aliasing.qual.Unique;

class A {
    static A lastA;

    public A() {
        lastA = this;
    }

    // :: warning: (inconsistent.constructor.type)
    public @Unique A(String s) {}
}

class B extends A {
    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    public @Unique B() {
        // :: error: (unique.leaked)
        super(); // "this" is aliased to A.lastA.
    }

    // :: warning: (inconsistent.constructor.type)
    public @Unique B(String s) {
        super(s); // no aliases created
    }
}
