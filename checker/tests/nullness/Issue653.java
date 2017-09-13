// Test case for Issue 653
// https://github.com/typetools/checker-framework/issues/653

// @skip-test Commented out until the bug is fixed

class Issue653 {

    public static @PolyAll String[] concat(
            @PolyAll String @Nullable [] a, @PolyAll String @Nullable [] b) {
        if (a == null) {
            if (b == null) {
                return new String[0];
            } else {
                return b;
            }
        } else {
            if (b == null) {
                return a;
            } else {
                @PolyAll String[] result = new String[a.length + b.length];

                System.arraycopy(a, 0, result, 0, a.length);
                System.arraycopy(b, 0, result, a.length, b.length);
                return result;
            }
        }
    }

    public static String[] debugTrackPpt = {};

    public static void add_track(String ppt) {
        String[] newArray = new String[] {ppt};
        debugTrackPpt = concat(debugTrackPpt, newArray);

        debugTrackPpt = concat(debugTrackPpt, new String[] {ppt});
    }
}
