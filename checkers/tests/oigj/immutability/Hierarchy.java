import checkers.oigj.quals.*;

/**
 * Smoke test to verify immutability type hierarchy
 */
class Hierarchy {
    @Immutable Object immutable;
    @Mutable Object mutable;
    @ReadOnly Object readOnly;

    void test() {
        //:: (type.incompatible)
        immutable = mutable;
        //:: (type.incompatible)
        immutable = readOnly;

        //:: (type.incompatible)
        mutable = immutable;
        //:: (type.incompatible)
        mutable = readOnly;

        readOnly = immutable;
        readOnly = mutable;
    }
}
