import org.checkerframework.checker.tainting.qual.*;

class Issue2159 {
    Issue2159() {}

    // :: warning: (inconsistent.constructor.type) :: error: (super.invocation.invalid)
    @PolyTainted Issue2159(@PolyTainted Object x) {}

    void testPolyTaintedLocal(
            @PolyTainted Object input, @Untainted Object untainted, @Tainted Object tainted) {
        // :: warning: (cast.unsafe)
        @PolyTainted Object local = (@PolyTainted Issue2159) new Issue2159();
        // :: warning: (cast.unsafe.constructor.invocation)
        @PolyTainted Object local1 = new @PolyTainted Issue2159();
        // :: warning: (cast.unsafe.constructor.invocation)
        @Untainted Object local2 = new @Untainted Issue2159();

        @PolyTainted Object local3 = new @PolyTainted Issue2159(input);
        // :: warning: (cast.unsafe.constructor.invocation)
        @Untainted Object local4 = new @Untainted Issue2159(input);
        // :: warning: (cast.unsafe.constructor.invocation)
        @PolyTainted Object local5 = new @PolyTainted Issue2159(tainted);
        @Untainted Object local6 = new @Untainted Issue2159(untainted);
    }
}
