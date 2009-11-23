import checkers.nullness.quals.*;
import java.util.*;


class PptTopLevel {
    /** List of all of the splitters for this ppt. */
    public @LazyNonNull List<Object> splitters = null;

    @AssertNonNullIfTrue("splitters")
    public boolean has_splitters() {
        return (splitters != null);
    }

    static void testPptTopLevel(PptTopLevel ppt) {
        if (!ppt.has_splitters())
            return;
        @NonNull Object s2 = ppt.splitters;
    }

    static void testPptTopLevelAssert(PptTopLevel ppt) {
        assert ppt.has_splitters();
        @NonNull Object s2 = ppt.splitters;
    }

    static void testSimple(PptTopLevel ppt) {
        if (ppt.has_splitters()) {
            @NonNull Object s2 = ppt.splitters;
        }
    }
}
