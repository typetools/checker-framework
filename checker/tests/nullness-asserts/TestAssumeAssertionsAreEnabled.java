import org.checkerframework.checker.nullness.qual.Nullable;

class TestAssumeAssertionsAreEnabled {

    void foo(@Nullable String s1, @Nullable String s2) {
        // :: error: (dereference.of.nullable)
        assert s2.equals(s1);
    }

    void bar(@Nullable String s1, @Nullable String s2) {
        // :: error: (dereference.of.nullable)
        assert s2.equals(s1) : "@AssumeAssertion(nullness)";
    }
}
