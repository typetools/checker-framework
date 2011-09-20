import checkers.igj.quals.*;

@I
public class Fields {
    @Assignable @I Fields left;
    @I Fields next;
    @ReadOnly Fields ro;

    Fields(@AssignsFields Fields this) { }

    Fields(@AssignsFields Fields this, int arg) {
        left = new @I Fields();
        next = new @Mutable Fields();   // should emit error
        next = new @Immutable Fields(); //  should emit error
        ro = this;
    }

    // Methods for testing
    static void isRO(@ReadOnly Fields f) { }
    static void isMutable(@Mutable Fields f) { }
    static void isImmutable(@Immutable Fields f) { }

    @I Fields getLeft(@ReadOnly Fields this) { return left; }
    @I Fields getNext(@ReadOnly Fields this) { return next; }
    @ReadOnly Fields getRO(@ReadOnly Fields this) { return ro; }

    void mutableReciever(@Mutable Fields this) {
        // all but ro should be mutable
        isRO(left);
        isRO(next);
        isRO(ro);

        // TestLeft
        isMutable(left);
        isMutable(this.left);
        isMutable(getLeft());

        isImmutable(left);  // should emit error
        isImmutable(this.left);  // should emit error
        isImmutable(getLeft());  // should emit error

        // Test Right
        isMutable(next);
        isMutable(this.next);
        isMutable(getNext());

        isImmutable(next);  // should emit error
        isImmutable(this.next);  // should emit error
        isImmutable(getNext());  // should emit error

        // Test RO
        isMutable(ro);  // should emit error
        isMutable(this.ro);  // should emit error
        isMutable(getRO());  // should emit error

        isImmutable(ro);  // should emit error
        isImmutable(this.ro);  // should emit error
        isImmutable(getRO());          // should emit error
    }

    void immutableReceiver(@Immutable Fields this) {
        // all but ro should be mutable
        isRO(left);
        isRO(next);
        isRO(ro);

        // TestLeft
        isMutable(left);  // should emit error
        isMutable(this.left);  // should emit error
        isMutable(getLeft());  // should emit error

        isImmutable(left);
        isImmutable(this.left);
        isImmutable(getLeft());

        // Test Right
        isMutable(next);  // should emit error
        isMutable(this.next);  // should emit error
        isMutable(getNext());  // should emit error

        isImmutable(next);
        isImmutable(this.next);
        isImmutable(getNext());

        // Test RO
        isMutable(ro);  // should emit error
        isMutable(this.ro);  // should emit error
        isMutable(getRO());  // should emit error

        isImmutable(ro);  // should emit error
        isImmutable(this.ro);   // should emit error
        isImmutable(getRO());   // should emit error
    }

    void roMethod(@ReadOnly Fields this) {
        // all but ro should be mutable
        isRO(left);
        isRO(next);
        isRO(ro);

        // TestLeft
        isMutable(left);  // should emit error
        isMutable(this.left);  // should emit error
        isMutable(getLeft());  // should emit error

        isImmutable(left);  // should emit error
        isImmutable(this.left);  // should emit error
        isImmutable(getLeft());  // should emit error

        // Test Right
        isMutable(next);  // should emit error
        isMutable(this.next);  // should emit error
        isMutable(getNext());  // should emit error

        isImmutable(next);  // should emit error
        isImmutable(this.next);  // should emit error
        isImmutable(getNext());  // should emit error

        // Test RO
        isMutable(ro);  // should emit error
        isMutable(this.ro);  // should emit error
        isMutable(getRO());  // should emit error

        isImmutable(ro);  // should emit error
        isImmutable(this.ro);  // should emit error
        isImmutable(getRO());  // should emit error
    }

    void assignsFieldsMethod(@AssignsFields Fields this) {
        // all but ro should be mutable
        isRO(left);
        isRO(next);
        isRO(ro);

        // TestLeft
        isMutable(left);  // should emit error
        isMutable(this.left);  // should emit error
        isMutable(getLeft());  // should emit error

        isImmutable(left);  // should emit error
        isImmutable(this.left);  // should emit error
        isImmutable(getLeft());  // should emit error

        // Test Right
        isMutable(next);  // should emit error
        isMutable(this.next);  // should emit error
        isMutable(getNext());  // should emit error

        isImmutable(next);  // should emit error
        isImmutable(this.next);  // should emit error
        isImmutable(getNext());  // should emit error

        // Test RO
        isMutable(ro);  // should emit error
        isMutable(this.ro);  // should emit error
        isMutable(getRO());  // should emit error

        isImmutable(ro);  // should emit error
        isImmutable(this.ro);  // should emit error
        isImmutable(getRO());  // should emit error
    }

    static void testNewMutableClass() {
        // all but ro should be mutable
        isRO(new @Mutable Fields().left);
        isRO(new @Mutable Fields().next);
        isRO(new @Mutable Fields().ro);

        // TestLeft
        isMutable(new @Mutable Fields().left);
        isMutable(new @Mutable Fields().getLeft());

        isImmutable(new @Mutable Fields().left);  // should emit error
        isImmutable(new @Mutable Fields().getLeft());  // should emit error

        // Test Right
        isMutable(new @Mutable Fields().next);
        isMutable(new @Mutable Fields().getNext());

        isImmutable(new @Mutable Fields().next);  // should emit error
        isImmutable(new @Mutable Fields().getNext());  // should emit error

        // Test RO
        isMutable(new @Mutable Fields().ro);  // should emit error
        isMutable(new @Mutable Fields().getRO());  // should emit error

        isImmutable(new @Mutable Fields().ro);  // should emit error
        isImmutable(new @Mutable Fields().getRO());  // should emit error
    }

    static void testNewImmutableClass() {
        // all but ro should be mutable
        isRO(new @Immutable Fields().left);
        isRO(new @Immutable Fields().next);
        isRO(new @Immutable Fields().ro);

        // TestLeft
        isMutable(new @Immutable Fields().left);  // should emit error
        isMutable(new @Immutable Fields().getLeft());  // should emit error

        isImmutable(new @Immutable Fields().left);
        isImmutable(new @Immutable Fields().getLeft());

        // Test Right
        isMutable(new @Immutable Fields().next);  // should emit error
        isMutable(new @Immutable Fields().getNext());  // should emit error

        isImmutable(new @Immutable Fields().next);
        isImmutable(new @Immutable Fields().getNext());

        // Test RO
        isMutable(new @Immutable Fields().ro);  // should emit error
        isMutable(new @Immutable Fields().getRO());  // should emit error

        isImmutable(new @Immutable Fields().ro);  // should emit error
        isImmutable(new @Immutable Fields().getRO());  // should emit error
    }

    static void testConditionalMutable() {
        @Mutable Fields mutable = null;
        // all but ro should be mutable
        isRO((true ? mutable : new @Mutable Fields()).left);
        isRO((true ? mutable : new @Mutable Fields()).next);
        isRO((true ? mutable : new @Mutable Fields()).ro);

        // TestLeft
        isMutable((true ? mutable : new @Mutable Fields()).left);
        isMutable((true ? mutable : new @Mutable Fields()).getLeft());

        isImmutable((true ? mutable : new @Mutable Fields()).left);  // should emit error
        isImmutable((true ? mutable : new @Mutable Fields()).getLeft());  // should emit error

        // Test Right
        isMutable((true ? mutable : new @Mutable Fields()).next);
        isMutable((true ? mutable : new @Mutable Fields()).getNext());

        isImmutable((true ? mutable : new @Mutable Fields()).next);  // should emit error
        isImmutable((true ? mutable : new @Mutable Fields()).getNext());  // should emit error

        // Test RO
        isMutable((true ? mutable : new @Mutable Fields()).ro);  // should emit error
        isMutable((true ? mutable : new @Mutable Fields()).getRO());  // should emit error

        isImmutable((true ? mutable : new @Mutable Fields()).ro);  // should emit error
        isImmutable((true ? mutable : new @Mutable Fields()).getRO());  // should emit error

    }

    static void testImmutableConditional() {
        @Immutable Fields immutable = null;
        // all but ro should be mutable
        isRO((true ? immutable : new @Immutable Fields()).left);
        isRO((true ? immutable : new @Immutable Fields()).next);
        isRO((true ? immutable : new @Immutable Fields()).ro);

        // TestLeft
        isMutable((true ? immutable : new @Immutable Fields()).left);  // should emit error
        isMutable((true ? immutable : new @Immutable Fields()).getLeft());  // should emit error

        isImmutable((true ? immutable : new @Immutable Fields()).left);
        isImmutable((true ? immutable : new @Immutable Fields()).getLeft());

        // Test Right
        isMutable((true ? immutable : new @Immutable Fields()).next);  // should emit error
        isMutable((true ? immutable : new @Immutable Fields()).getNext());  // should emit error

        isImmutable((true ? immutable : new @Immutable Fields()).next);
        isImmutable((true ? immutable : new @Immutable Fields()).getNext());

        // Test RO
        isMutable((true ? immutable : new @Immutable Fields()).ro);  // should emit error
        isMutable((true ? immutable : new @Immutable Fields()).getRO());  // should emit error

        isImmutable((true ? immutable : new @Immutable Fields()).ro);  // should emit error
        isImmutable((true ? immutable : new @Immutable Fields()).getRO());  // should emit error
    }

}
