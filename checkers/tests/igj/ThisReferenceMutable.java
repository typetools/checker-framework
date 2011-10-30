import checkers.igj.quals.*;

@I
class ThisReferenceMutableSuper {

    // Receiving methods
    public static void isRO(@ReadOnly ThisReferenceMutableSuper obj) { }
    public static void isMutable(@Mutable ThisReferenceMutableSuper obj) { }
    public static void isImmutable(@Immutable ThisReferenceMutableSuper obj) { }

}

class MutableThisTest extends ThisReferenceMutableSuper {
    public void testRO(@ReadOnly MutableThisTest this) {
        isRO(this);
        isMutable(this);   // should emit error
        isImmutable(this); // should emit error
    }

    public void testAssignsFields (@AssignsFields MutableThisTest this) {
        isRO(this);
        isMutable(this);   // should emit error
        isImmutable(this); // should emit error
    }

    public void testMutable(@Mutable MutableThisTest this) {
        isRO(this);
        isMutable(this);
        isImmutable(this); // should emit error
    }
}
