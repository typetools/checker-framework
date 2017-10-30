import org.checkerframework.checker.nullness.qual.*;

class TestPolyNull {
    @PolyNull String identity(@PolyNull String str) {
        return str;
    }

    void test1() {
        identity(null);
    }

    void test2() {
        identity((@Nullable String) null);
    }

    public static @PolyNull String[] typeArray(@PolyNull Object[] seq, @Nullable String nullable) {
        @SuppressWarnings("nullness") // ignore array initialization here.
        @PolyNull String[] retval = new @Nullable String[seq.length];
        for (int i = 0; i < seq.length; i++) {
            if (seq[i] == null) {
                // null can be assigned into the PolyNull array, because we
                // performed a test on seq and know that it is nullable.
                retval[i] = null;
                // and so can something that is nullable
                retval[i] = nullable;
                // One can always add a dummy value: nonnull is the bottom
                // type and legal for any instantiation of PolyNull.
                retval[i] = "dummy";
            } else {
                retval[i] = seq[i].getClass().toString();
                // :: error: (assignment.type.incompatible)
                retval[i] = null;
                // :: error: (assignment.type.incompatible)
                retval[i] = nullable;
            }
        }
        return retval;
    }

    public static @PolyNull String identity2(@PolyNull String a) {
        // TODO: it would be nice, if this code type-checks (just like identity and identity3),
        // but currently a technical limitation in the flow analysis prevents this
        // :: error: (return.type.incompatible)
        return (a == null) ? null : a;
    }

    public static @PolyNull String identity3(@PolyNull String a) {
        if (a == null) {
            return null;
        }
        return a;
    }
}
