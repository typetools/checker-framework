import checkers.nullness.quals.*;

public class AssertParameterNullness {

    /** True iff both sequences are non-null and have the same length. */
    @AssertNonNullIfTrue({"#0", "#1"})
    /* pure */ public static boolean sameLength(boolean @Nullable [] seq1, boolean @Nullable [] seq2) {
        return ((seq1 != null)
                && (seq2 != null)
                && seq1.length == seq2.length);
    }

    /* pure */ public static boolean pairwiseEqual(boolean @Nullable [] seq3, boolean @Nullable [] seq4) {
        if (sameLength(seq3, seq4)) {
            boolean b1 = seq3[0];
            boolean b2 = seq4[0];
        } else {
            //:: error: (accessing.nullable)
            boolean b1 = seq3[0];
            //:: error: (accessing.nullable)
            boolean b2 = seq4[0];
        }
        return true;
    }

}
