import checkers.igj.quals.*;

@I
class ThisReferenceImmutableSuper {

    // Receiving methods
    public static void isRO(@ReadOnly ThisReferenceImmutableSuper obj) { }
    public static void isMutable(@Mutable ThisReferenceImmutableSuper obj) { }
    public static void isImmutable(@Immutable ThisReferenceImmutableSuper obj) { }

}


@Immutable
class ImmutableThisTest extends ThisReferenceImmutableSuper {
    public void testRO() @ReadOnly {
        isRO(this);
        isMutable(this);   // should emit error
        isImmutable(this);
    }

    public void testAssignsFields () @AssignsFields {
        isRO(this);
        isMutable(this);   // should emit error
        isImmutable(this);
    }

    public void testImmutable() @Immutable {
        isRO(this);
        isMutable(this);   // should emit error
        isImmutable(this);
    }
}
