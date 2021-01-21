import org.checkerframework.checker.nullness.qual.*;

public class UnannoPrimitives {
    // :: error: (nullness.on.primitive)
    @Nullable int f;

    // :: error: (nullness.on.primitive)
    @NonNull int g;

    void local() {
        // test whether an arbitrary declaration annotation gets confused
        @SuppressWarnings("tata")
        int h = Integer.valueOf(5);

        int i = Integer.valueOf(99) + 1900;
        int j = 7 + 1900;

        // :: error: (nullness.on.primitive)
        @Nullable int f;

        // :: error: (nullness.on.primitive)
        @NonNull int g;
    }

    static void testDate() {
        @SuppressWarnings("deprecation") // for iCal4j
        int year = new java.util.Date().getYear() + 1900;
        String strDate = "/" + year;
    }

    // :: error: (nullness.on.primitive)
    @Nullable byte[] d1 = {4};
    byte @Nullable [] d1b = {4};

    // :: error: (nullness.on.primitive)
    @Nullable byte[][] twoD = {{4}};

    // :: error: (nullness.on.primitive)
    @Nullable byte[][][] threeD = {{{4}}};

    // :: error: (nullness.on.primitive)
    @Nullable byte[][][][] fourD = {{{{4}}}};

    @SuppressWarnings("ha!")
    byte[] d2 = {4};

    // :: error: (nullness.on.primitive)
    Object ar = new @Nullable byte[] {4};

    // :: error: (nullness.on.primitive)
    Object ar2 = new @NonNull byte[] {42};

    void testCasts(Integer i1) {
        Object i2 = (int) i1;
        // :: error: (nullness.on.primitive)
        Object i3 = (@Nullable int) i1;
    }
}
