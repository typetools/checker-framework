
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
}
