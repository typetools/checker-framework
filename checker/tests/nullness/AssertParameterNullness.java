import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

public class AssertParameterNullness {

  /** True iff both sequences are non-null and have the same length. */
  @EnsuresNonNullIf(
      result = true,
      expression = {"#1", "#2"})
  /* pure */ public static boolean sameLength(
      final boolean @Nullable [] seq1, final boolean @Nullable [] seq2) {
    if ((seq1 != null) && (seq2 != null) && seq1.length == seq2.length) {
      return true;
    }
    return false;
  }

  /* pure */ public static boolean pairwiseEqual(
      boolean @Nullable [] seq3, boolean @Nullable [] seq4) {
    if (sameLength(seq3, seq4)) {
      boolean b1 = seq3[0];
      boolean b2 = seq4[0];
    } else {
      // :: error: (accessing.nullable)
      boolean b1 = seq3[0];
      // :: error: (accessing.nullable)
      boolean b2 = seq4[0];
    }
    return true;
  }
}
