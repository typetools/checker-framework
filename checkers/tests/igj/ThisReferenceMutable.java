import checkers.igj.quals.*;

@I
class ThisReferenceMutableSuper {

    // Receiving methods
    public static void isRO(@ReadOnly ThisReferenceMutableSuper obj) { }
    public static void isMutable(@Mutable ThisReferenceMutableSuper obj) { }
    public static void isImmutable(@Immutable ThisReferenceMutableSuper obj) { }

}

@Mutable
class MutableThisTest extends ThisReferenceMutableSuper {
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
}