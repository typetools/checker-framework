import java.util.List;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

class PptTopLevel {
    /** List of all of the splitters for this ppt. */
    public @MonotonicNonNull List<Object> splitters = null;

    @EnsuresNonNullIf(result = true, expression = "splitters")
    public boolean has_splitters() {
        return (splitters != null);
    }

    static void testPptTopLevel(PptTopLevel ppt) {
        if (!ppt.has_splitters()) {
            return;
        }
        @NonNull Object s2 = ppt.splitters;
    }

    static void testPptTopLevelAssert(PptTopLevel ppt) {
        assert ppt.has_splitters() : "@AssumeAssertion(nullness)";
        @NonNull Object s2 = ppt.splitters;
    }

    static void testSimple(PptTopLevel ppt) {
        if (ppt.has_splitters()) {
            @NonNull Object s2 = ppt.splitters;
        }
    }

    // False tests
    static void testFalse(PptTopLevel ppt) {
        // :: error: (dereference.of.nullable)
        ppt.splitters.toString(); // error
    }

    static void testFalseNoAssertion(PptTopLevel ppt) {
        ppt.has_splitters();
        // :: error: (dereference.of.nullable)
        ppt.splitters.toString(); // error
    }

    static void testFalseIf(PptTopLevel ppt) {
        if (ppt.has_splitters()) {
            return;
        }
        // :: error: (dereference.of.nullable)
        ppt.splitters.toString(); // error
    }

    //    static void testFalseIfBody(PptTopLevel ppt) {
    //        if (!ppt.has_splitters()) {
    //            // :: error: (dereference.of.nullable)
    //            ppt.splitters.toString();   // error
    //        }
    //    }
}
