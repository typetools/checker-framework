import polyall.quals.*;

class Constructors {
    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    @H1S2 @H2S2 Constructors() {}

    void test1() {
        // All quals from constructor
        @H1S2 @H2S2 Constructors c1 = new Constructors();
        // Can still specify my own
        @H1S2 @H2Top Constructors c2 = new @H1S2 @H2Top Constructors();
        // Can only specify some of the qualifiers, rest comes
        // from constructor
        @H1S2 @H2S2 Constructors c3 = new @H1S2 Constructors();

        // :: error: (assignment.type.incompatible)
        @H1S2 @H2S1 Constructors e1 = new Constructors();
        // :: error: (assignment.type.incompatible)
        @H1S2 @H2S1 Constructors e2 = new @H1S2 Constructors();
    }

    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    @H1S2 @H2Poly Constructors(@H1S1 @H2Poly int i) {}

    void test2(@H1S1 @H2S2 int p) {
        @H1S2 @H2S2 Constructors c1 = new Constructors(p);
        @H1S2 @H2S2 Constructors c2 = new @H1S2 @H2S2 Constructors(p);
        @H1S2 @H2S2 Constructors c3 = new @H1S2 Constructors(p);

        // :: error: (assignment.type.incompatible)
        @H1S2 @H2S1 Constructors e1 = new Constructors(p);
        // :: error: (assignment.type.incompatible)
        @H1S2 @H2S1 Constructors e2 = new @H1S2 @H2S2 Constructors(p);
        // :: error: (assignment.type.incompatible)
        @H1S2 @H2S1 Constructors e3 = new @H1S2 Constructors(p);
    }

    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    @H1Poly @H2Poly Constructors(@H1Poly @H2Poly String s) {}

    void test3(@H1S1 @H2S2 String p) {
        @H1S1 @H2S2 Constructors c1 = new Constructors(p);
        @H1S1 @H2S2 Constructors c2 = new @H1S1 @H2S2 Constructors(p);
        @H1S1 @H2S2 Constructors c3 = new @H1S1 Constructors(p);

        // :: error: (assignment.type.incompatible)
        @H1S2 @H2S1 Constructors e1 = new Constructors(p);
        // :: error: (assignment.type.incompatible) :: error: (constructor.invocation.invalid)
        @H1S2 @H2S1 Constructors e2 = new @H1S2 @H2S2 Constructors(p);
        // :: error: (assignment.type.incompatible) :: error: (constructor.invocation.invalid)
        @H1S2 @H2S1 Constructors e3 = new @H1S2 Constructors(p);

        // :: error: (constructor.invocation.invalid)
        @H1S2 @H2S2 Constructors e4 = new @H1S2 @H2S2 Constructors(p);
        // :: error: (constructor.invocation.invalid)
        @H1S2 @H2S2 Constructors e5 = new @H1S2 Constructors(p);
    }

    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    @testlib.util.Encrypted @H1Poly @H2Poly Constructors(@H1Poly @H2Poly String s, int i) {}

    void test4(@H1S1 @H2S2 String p) {
        @H1S1 @H2S2 Constructors c1 = new Constructors(p, 4);
        @H1S1 @H2S2 Constructors c2 = new @testlib.util.Encrypted Constructors(p);
    }
}
