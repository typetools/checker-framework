public class Arrays {
    public static final String[] RELATIONSHIP_LABELS = {
        "SJJ", "SJU", "SUJ", "SUU", "DJJ", "DJU", "DUJ", "DUU", "JM", "UM", "MJ", "MU"
    };

    public static final int[] ia = {1, -2, 3};

    // Note that "-1.0" is _NOT_ a double literal! It's a unary minus,
    // that needs to be handled correctly.
    public static final double[] elts_plus_minus_one_float = {-1.0, 1.0, +1.0, 1.0 / 2.0};

    String[] vis = new String[] {"a", "b"};

    @SuppressWarnings("nullness") // Don't want to depend on @Nullable
    void m() {
        class VarInfo {}
        VarInfo v1 = null;
        VarInfo v2 = null;
        VarInfo[] vis = null;

        if (v2 == null) {
            vis = new VarInfo[] {v1};
        } else {
            vis = new VarInfo[] {v1, v2};
        }
    }
}
