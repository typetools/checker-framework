import checkers.nullness.quals.*;

class NullnessAssertion {

    void test1() {
        Object o = null;
        assertNonNull(o);
        o.toString();

    }

    void test2() {
        Object o = null;
        o.toString();
        assertNonNull(o);
    }

    @AssertNonNull // the method an throws exception if any argument is null
    void assertNonNull(@Nullable Object o) {
        if (o == null)
            throw new NullPointerException();
    }
}
