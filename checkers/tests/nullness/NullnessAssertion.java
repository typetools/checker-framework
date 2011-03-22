import checkers.nullness.quals.*;

class NullnessAssertion {

    void test1() {
        Object o = null;
        assertNonNull(o);
        o.toString();

    }

    void test2() {
        Object o = null;
        //:: error: (dereference.of.nullable)
        o.toString();
        assertNonNull(o);
    }

    @AssertParametersNonNull // the method an throws exception if any argument is null
    void assertNonNull(@Nullable Object o) {
        if (o == null)
            throw new NullPointerException();
    }
}
