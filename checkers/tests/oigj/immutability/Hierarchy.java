import checkers.oigj.quals.*;

/**
 * Smoke test to verify immutability type hierarchy
 */
class Hierarchy {
    @Immutable Object immutable;
    @Mutable Object mutable;
    @ReadOnly Object readOnly;

    void test() {
        //:: error: (assignment.type.incompatible)
        immutable = mutable;
        //:: error: (assignment.type.incompatible)
        immutable = readOnly;

        //:: error: (assignment.type.incompatible)
        mutable = immutable;
        //:: error: (assignment.type.incompatible)
        mutable = readOnly;

        readOnly = immutable;
        readOnly = mutable;
    }
}
