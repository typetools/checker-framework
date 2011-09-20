import checkers.igj.quals.*;

@I
class ThisReference {

    // Receiving methods
    public static void isRO(@ReadOnly ThisReference obj) { }
    public static void isMutable(@Mutable ThisReference obj) { }
    public static void isImmutable(@Immutable ThisReference obj) { }

    public void testRO(@ReadOnly ThisReference this) {
        isRO(this);
        isMutable(this);   // should emit error
        isImmutable(this); // should emit error
    }

    public void testAssignsFields (@AssignsFields ThisReference this) {
        isRO(this);
        isMutable(this);   // should emit error
        isImmutable(this); // should emit error
    }

    public void testMutable(@Mutable ThisReference this) {
        isRO(this);
        isMutable(this);
        isImmutable(this); // should emit error
    }

    public void testImmutable(@Immutable ThisReference this) {
        isRO(this);
        isMutable(this);   // should emit error
        isImmutable(this);
    }
}
