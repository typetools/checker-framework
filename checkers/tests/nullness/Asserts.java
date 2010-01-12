import checkers.nullness.quals.*;

public class Asserts {

    void propogateToExpr() {
        String s = "m";
        assert false : s.getClass();
    }

    // @skip-test fix me in a bit
//    void incorrectAssertExpr() {
//        String s = null;
//        //:: (dereference.of.nullable)
//        assert s != null : s.getClass() + " suppress nullness";  // error
//        s.getClass();  // OK
//    }

    void correctAssertExpr() {
        String s = null;
        assert s == null : s.getClass() + " suppress nullness";
        //:: (dereference.of.nullable)
        s.getClass();   // error
    }

    class ArrayCell {
        @Nullable Object[] vals;
    }

    void assertComplexExpr (ArrayCell ac, int i) {
        assert ac.vals[i] != null : "@SuppressWarnings(nullness)";
        @NonNull Object o = ac.vals[i];
    }

    boolean pairwiseEqual(boolean @Nullable [] seq1, boolean @Nullable [] seq2) {
        if (! sameLength(seq1, seq2)) { return false; }
        if (ne(seq1[0], seq2[0]));
        return true;
      }

      @AssertNonNullIfTrue({"#0", "#1"})
      boolean sameLength(boolean @Nullable [] seq1, boolean @Nullable [] seq2) {
          return true;
      }

      static boolean ne(boolean a, boolean b) { return true; }

}
