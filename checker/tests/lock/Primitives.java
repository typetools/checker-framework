// @skip-test
// TODO: Reenable this test after a @GuardedByName annotation is implemented that can guard primitives,
// and uncomment all the @GuardedByName annotations below.

// Note that testing of the immutable.type.guardedby error message is done in TestTreeKinds.java

class Primitives {
    //@GuardedByName("lock")
    int primitive = 1;

    //@GuardedByName("lock")
    boolean primitiveBoolean;

    public void testOperationsWithPrimitives() {
        //@GuardedByName("lock")
        int i = 0;
        //@GuardedByName("lock")
        boolean b;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = i >>> primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = primitive >>> i;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i >>>= primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        primitive >>>= i;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i %= primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = 4 % primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = primitive % 4;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        primitive++;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        primitive--;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        ++primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        --primitive;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        if (primitive != 5) {}

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = primitive >> i;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = primitive << i;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = i >> primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = i << primitive;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i <<= primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i >>= primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        primitive <<= i;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        primitive >>= i;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        assert (primitiveBoolean);

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = primitive >= i;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = primitive <= i;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = primitive > i;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = primitive < i;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = i >= primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = i <= primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = i > primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = i < primitive;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i += primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i -= primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i *= primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i /= primitive;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = 4 + primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = 4 - primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = 4 * primitive;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = 4 / primitive;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = primitive + 4;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = primitive - 4;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = primitive * 4;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = primitive / 4;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        if (primitiveBoolean) {}

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = ~primitive;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = primitiveBoolean || false;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = primitiveBoolean | false;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = primitiveBoolean ^ true;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b &= primitiveBoolean;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b |= primitiveBoolean;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b ^= primitiveBoolean;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = !primitiveBoolean;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        i = primitive;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = true && primitiveBoolean;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = true & primitiveBoolean;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = false || primitiveBoolean;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = false | primitiveBoolean;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = false ^ primitiveBoolean;

        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = primitiveBoolean && true;
        // TODO reenable this error: (contracts.precondition.not.satisfied.field)
        b = primitiveBoolean & true;
    }
}
