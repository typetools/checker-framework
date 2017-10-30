import org.checkerframework.checker.nullness.qual.*;

class Unboxing {

    @Nullable Integer f;

    public void t1() {
        // :: error: (unboxing.of.nullable)
        @NonNull int l = f + 1;
        // no error, since f has been unboxed
        f.toString();
    }

    public void t2() {
        try {
            // :: error: (unboxing.of.nullable)
            @NonNull int l = f + 1;
        } catch (NullPointerException npe) {
            // f is known to be null on the exception edge
            // :: error: (unboxing.of.nullable)
            @NonNull int m = f + 1;
        }
        // after the merge, f cannot be null
        f.toString();
    }

    void foo(@Nullable Integer in) {
        // :: error: (unboxing.of.nullable)
        int q = in;
    }

    int bar(@Nullable Integer in) {
        // :: error: (unboxing.of.nullable)
        return in;
    }

    <T extends @Nullable Integer> int barT(T in) {
        // :: error: (unboxing.of.nullable)
        int q = in;
        // :: error: (unboxing.of.nullable)
        return in;
    }
}
