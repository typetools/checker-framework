import checkers.nullness.quals.*;

public class Asserts {

    void propogateToExpr() {
        String s = "m";
        assert false : s.getClass();
    }

    void incorrectAssertExpr() {
        String s = null;
        assert s != null : s.getClass() + " suppress nullness";  // error
        s.getClass();  // OK
    }

    void correctAssertExpr() {
        String s = null;
        assert s == null : s.getClass() + " suppress nullness";
        s.getClass();   // error
    }

    class ArrayCell {
        /*@Nullable*/ Object[] vals;
    }

    void assertComplexExpr (ArrayCell ac, int i) {
        assert ac.vals[i] != null : "@SuppressWarnings(nullness)";
        /*@NonNull*/ Object o = ac.vals[i];
    }

}
