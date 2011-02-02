import checkers.igj.quals.*;

@I
class ThisReference {

    // Receiving methods
    public static void isRO(@ReadOnly ThisReference obj) { }
    public static void isMutable(@Mutable ThisReference obj) { }
    public static void isImmutable(@Immutable ThisReference obj) { }

    public void testRO() @ReadOnly {
        isRO(this);
        isMutable(this);   // should emit error
        isImmutable(this); // should emit error
    }

    public void testAssignsFields () @AssignsFields {
        isRO(this);
        isMutable(this);   // should emit error
        isImmutable(this); // should emit error
    }

    public void testMutable() @Mutable {
        isRO(this);
        isMutable(this);
        isImmutable(this); // should emit error
    }

    public void testImmutable() @Immutable {
        isRO(this);
        isMutable(this);   // should emit error
        isImmutable(this);
    }
}
