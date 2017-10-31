import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.framework.qual.PolyAll;

// Same test as TestPolyNull, just using PolyAll as qualifier.
// Behavior must be the same.
class TestPolyAll {
    @PolyAll String identity(@PolyAll String str) {
        return str;
    }

    void test1() {
        identity(null);
    }

    void test2() {
        identity((@Nullable String) null);
    }

    public static @PolyNull String[] typeArray(@PolyNull Object[] seq) {
        @PolyNull String[] retval = new @PolyNull String[seq.length];
        for (int i = 0; i < seq.length; i++) {
            if (seq[i] == null) {
                retval[i] = null;
                retval[i] = "ok";
            } else {
                retval[i] = seq[i].getClass().toString();
                // :: error: (assignment.type.incompatible)
                retval[i] = null;
            }
        }
        return retval;
    }
}
