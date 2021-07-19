import org.checkerframework.checker.nullness.qual.Nullable;

public class TestAssumeAssertionsAreDisabled {

    void foo(@Nullable String s1, @Nullable String s2) {

        // If assertions are disabled, then this cannot throw a NullPointerException
        assert s2.equals(s1);

        // However, even with assertions disabled, @AssumeAssertion is still respected
        // :: error: (dereference.of.nullable)
        assert s2.equals(s1) : "@AssumeAssertion(nullness)";
    }
}
