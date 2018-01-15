import org.checkerframework.checker.nullness.qual.*;

class UnannoPrimitives {
    // :: error: (type.invalid.annotations.on.use)
    @Nullable int f;

    // TODO:: error: (type.invalid)
    @NonNull int g;

    void local() {
        // test whether an arbitrary declaration annotation gets confused
        @SuppressWarnings("tata")
        int h = new Integer(5);

        int i = new Integer(99) + 1900;
        int j = 7 + 1900;

        // :: error: (type.invalid.annotations.on.use)
        @Nullable int f;

        // TODO:: error: (type.invalid)
        @NonNull int g;
    }

    static void testDate() {
        @SuppressWarnings("deprecation") // for iCal4j
        int year = new java.util.Date().getYear() + 1900;
        String strDate = "/" + year;
    }

    // :: error: (type.invalid.annotations.on.use)
    @Nullable byte[] d1 = {4};
    byte @Nullable [] d1b = {4};

    @SuppressWarnings("ha!")
    byte[] d2 = {4};

    // :: error: (type.invalid.annotations.on.use)
    Object ar = new @Nullable byte[] {4};

    // TODO:: error: (type.invalid)
    Object ar2 = new @NonNull byte[] {42};

    void testCasts(Integer i1) {
        Object i2 = (int) i1;
        // TODO:: error: (type.invalid)
        Object i3 = (@Nullable int) i1;
    }
}
