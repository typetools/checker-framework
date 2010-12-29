import checkers.oigj.quals.*;

/**
 * Smoke test to verify immutability type hierarchy
 */
class Hierarchy {
    @Immutable Object immutable;
    @Mutable Object mutable;
    @ReadOnly Object readOnly;

    void test() {
        //:: (assignment.type.incompatible)
        immutable = mutable;
        //:: (assignment.type.incompatible)
        immutable = readOnly;

        //:: (assignment.type.incompatible)
        mutable = immutable;
        //:: (assignment.type.incompatible)
        mutable = readOnly;

        readOnly = immutable;
        readOnly = mutable;
    }
}
