// @skip-test
// TODO: Re-enable this test after a @GuardedByName annotation is implemented that can guard
// primitives, and uncomment all the @GuardedByName annotations below.

// Note that testing of the immutable.type.guardedby error message is done in TestTreeKinds.java

public class PrimitivesLocking {
  // @GuardedByName("lock")
  int primitive = 1;

  // @GuardedByName("lock")
  boolean primitiveBoolean;

  public void testOperationsWithPrimitives() {
    // @GuardedByName("lock")
    int i = 0;
    // @GuardedByName("lock")
    boolean b;

    // TODO re-enable this error: [lock.not.held]
    i = i >>> primitive;
    // TODO re-enable this error: [lock.not.held]
    i = primitive >>> i;

    // TODO re-enable this error: [lock.not.held]
    i >>>= primitive;
    // TODO re-enable this error: [lock.not.held]
    primitive >>>= i;

    // TODO re-enable this error: [lock.not.held]
    i %= primitive;
    // TODO re-enable this error: [lock.not.held]
    i = 4 % primitive;
    // TODO re-enable this error: [lock.not.held]
    i = primitive % 4;

    // TODO re-enable this error: [lock.not.held]
    primitive++;
    // TODO re-enable this error: [lock.not.held]
    primitive--;
    // TODO re-enable this error: [lock.not.held]
    ++primitive;
    // TODO re-enable this error: [lock.not.held]
    --primitive;

    // TODO re-enable this error: [lock.not.held]
    if (primitive != 5) {}

    // TODO re-enable this error: [lock.not.held]
    i = primitive >> i;
    // TODO re-enable this error: [lock.not.held]
    i = primitive << i;
    // TODO re-enable this error: [lock.not.held]
    i = i >> primitive;
    // TODO re-enable this error: [lock.not.held]
    i = i << primitive;

    // TODO re-enable this error: [lock.not.held]
    i <<= primitive;
    // TODO re-enable this error: [lock.not.held]
    i >>= primitive;
    // TODO re-enable this error: [lock.not.held]
    primitive <<= i;
    // TODO re-enable this error: [lock.not.held]
    primitive >>= i;

    // TODO re-enable this error: [lock.not.held]
    assert (primitiveBoolean);

    // TODO re-enable this error: [lock.not.held]
    b = primitive >= i;
    // TODO re-enable this error: [lock.not.held]
    b = primitive <= i;
    // TODO re-enable this error: [lock.not.held]
    b = primitive > i;
    // TODO re-enable this error: [lock.not.held]
    b = primitive < i;
    // TODO re-enable this error: [lock.not.held]
    b = i >= primitive;
    // TODO re-enable this error: [lock.not.held]
    b = i <= primitive;
    // TODO re-enable this error: [lock.not.held]
    b = i > primitive;
    // TODO re-enable this error: [lock.not.held]
    b = i < primitive;

    // TODO re-enable this error: [lock.not.held]
    i += primitive;
    // TODO re-enable this error: [lock.not.held]
    i -= primitive;
    // TODO re-enable this error: [lock.not.held]
    i *= primitive;
    // TODO re-enable this error: [lock.not.held]
    i /= primitive;

    // TODO re-enable this error: [lock.not.held]
    i = 4 + primitive;
    // TODO re-enable this error: [lock.not.held]
    i = 4 - primitive;
    // TODO re-enable this error: [lock.not.held]
    i = 4 * primitive;
    // TODO re-enable this error: [lock.not.held]
    i = 4 / primitive;

    // TODO re-enable this error: [lock.not.held]
    i = primitive + 4;
    // TODO re-enable this error: [lock.not.held]
    i = primitive - 4;
    // TODO re-enable this error: [lock.not.held]
    i = primitive * 4;
    // TODO re-enable this error: [lock.not.held]
    i = primitive / 4;

    // TODO re-enable this error: [lock.not.held]
    if (primitiveBoolean) {}

    // TODO re-enable this error: [lock.not.held]
    i = ~primitive;

    // TODO re-enable this error: [lock.not.held]
    b = primitiveBoolean || false;
    // TODO re-enable this error: [lock.not.held]
    b = primitiveBoolean | false;

    // TODO re-enable this error: [lock.not.held]
    b = primitiveBoolean ^ true;

    // TODO re-enable this error: [lock.not.held]
    b &= primitiveBoolean;

    // TODO re-enable this error: [lock.not.held]
    b |= primitiveBoolean;

    // TODO re-enable this error: [lock.not.held]
    b ^= primitiveBoolean;

    // TODO re-enable this error: [lock.not.held]
    b = !primitiveBoolean;

    // TODO re-enable this error: [lock.not.held]
    i = primitive;

    // TODO re-enable this error: [lock.not.held]
    b = true && primitiveBoolean;
    // TODO re-enable this error: [lock.not.held]
    b = true & primitiveBoolean;

    // TODO re-enable this error: [lock.not.held]
    b = false || primitiveBoolean;
    // TODO re-enable this error: [lock.not.held]
    b = false | primitiveBoolean;

    // TODO re-enable this error: [lock.not.held]
    b = false ^ primitiveBoolean;

    // TODO re-enable this error: [lock.not.held]
    b = primitiveBoolean && true;
    // TODO re-enable this error: [lock.not.held]
    b = primitiveBoolean & true;
  }
}
