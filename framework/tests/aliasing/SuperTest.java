import org.checkerframework.common.aliasing.qual.Unique;

class A {
    static A lastA;

    public A() {
        lastA = this;
    }

    public @Unique A(String s) {}
}

class B extends A {
    public @Unique B() {
        // :: error: (unique.leaked)
        super(); // "this" is aliased to A.lastA.
    }

    public @Unique B(String s) {
        super(s); // no aliases created
    }
}
