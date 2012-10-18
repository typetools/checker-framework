import checkers.oigj.quals.*;

public class FieldAccesses {
    static FieldAccesses other;
    // Helper method to wrap access expressions as statements
    void access(@World Object o) { }

    @Dominator Object dominator;

    /**
     * For o.f = ..., if O(f) = dominator and o != this, the
     * access is illegal
     */
    void testCaseI() {
        access(this.dominator);
        access(dominator);

        //:: error: (unallowed.access)
        access(other.dominator);
    }
}
