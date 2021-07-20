import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

import java.util.List;

public class NegatingConditionalNullness {
    public @MonotonicNonNull List<Object> splitters = null;

    @EnsuresNonNullIf(result = true, expression = "splitters")
    public boolean has_splitters() {
        return (splitters != null);
    }

    static void test(NegatingConditionalNullness ppt) {
        if (!ppt.has_splitters()) {
            return;
        }
        @NonNull Object s2 = ppt.splitters;
    }

    static void testAssert(NegatingConditionalNullness ppt) {
        assert ppt.has_splitters() : "@AssumeAssertion(nullness)";
        @NonNull Object s2 = ppt.splitters;
    }

    static void testSimple(NegatingConditionalNullness ppt) {
        if (ppt.has_splitters()) {
            @NonNull Object s2 = ppt.splitters;
        }
    }

    // False tests
    static void testFalse(NegatingConditionalNullness ppt) {
        // :: error: (dereference.of.nullable)
        ppt.splitters.toString(); // error
    }

    static void testFalseNoAssertion(NegatingConditionalNullness ppt) {
        ppt.has_splitters();
        // :: error: (dereference.of.nullable)
        ppt.splitters.toString(); // error
    }

    static void testFalseIf(NegatingConditionalNullness ppt) {
        if (ppt.has_splitters()) {
            return;
        }
        // :: error: (dereference.of.nullable)
        ppt.splitters.toString(); // error
    }

    //    static void testFalseIfBody(NegatingConditionalNullness ppt) {
    //        if (!ppt.has_splitters()) {
    //            // :: error: (dereference.of.nullable)
    //            ppt.splitters.toString();   // error
    //        }
    //    }
}
