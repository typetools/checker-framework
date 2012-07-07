import checkers.igj.quals.*;

@I
public class MethodInvocation {

    @Mutable MethodInvocation mutable;
    @Immutable MethodInvocation immutable;
    @ReadOnly MethodInvocation readOnly;

    void mutableReciever(@Mutable MethodInvocation this) { }
    void immutableReceiver(@Immutable MethodInvocation this) { }
    void readOnlyReceiver(@ReadOnly MethodInvocation this) { }
    void assignsFieldsMethod(@AssignsFields MethodInvocation this) { }

    void testMutable(@Mutable MethodInvocation this) {
        mutableReciever();
        immutableReceiver();  // should emit error
        readOnlyReceiver();
        assignsFieldsMethod();

        this.mutableReciever();
        this.immutableReceiver();  // should emit error
        this.readOnlyReceiver();
        this.assignsFieldsMethod();

        mutable.mutableReciever();
        mutable.immutableReceiver();  // should emit error
        mutable.readOnlyReceiver();
        mutable.assignsFieldsMethod();

        immutable.mutableReciever();  // should emit error
        immutable.immutableReceiver();
        immutable.readOnlyReceiver();
        immutable.assignsFieldsMethod();  // should emit error

        readOnly.mutableReciever();  // should emit error
        readOnly.immutableReceiver();  // should emit error
        readOnly.readOnlyReceiver();
        readOnly.assignsFieldsMethod();  // should emit error
    }

    void testImmutable(@Immutable MethodInvocation this) {
        mutableReciever();  // should emit error
        immutableReceiver();
        readOnlyReceiver();
        assignsFieldsMethod();  // should emit error

        this.mutableReciever();  // should emit error
        this.immutableReceiver();
        this.readOnlyReceiver();
        this.assignsFieldsMethod();  // should emit error

        mutable.mutableReciever();
        mutable.immutableReceiver();  // should emit error
        mutable.readOnlyReceiver();
        mutable.assignsFieldsMethod();

        immutable.mutableReciever();  // should emit error
        immutable.immutableReceiver();
        immutable.readOnlyReceiver();
        immutable.assignsFieldsMethod();  // should emit error

        readOnly.mutableReciever();  // should emit error
        readOnly.immutableReceiver();  // should emit error
        readOnly.readOnlyReceiver();
        readOnly.assignsFieldsMethod();  // should emit error
    }

    void testReadOnly(@ReadOnly MethodInvocation this) {
        mutableReciever();  // should emit error
        immutableReceiver();  // should emit error
        readOnlyReceiver();
        assignsFieldsMethod();  // should emit error

        this.mutableReciever();  // should emit error
        this.immutableReceiver();  // should emit error
        this.readOnlyReceiver();
        this.assignsFieldsMethod();  // should emit error

        mutable.mutableReciever();
        mutable.immutableReceiver();  // should emit error
        mutable.readOnlyReceiver();
        mutable.assignsFieldsMethod();

        immutable.mutableReciever();  // should emit error
        immutable.immutableReceiver();
        immutable.readOnlyReceiver();
        immutable.assignsFieldsMethod();  // should emit error

        readOnly.mutableReciever();  // should emit error
        readOnly.immutableReceiver();  // should emit error
        readOnly.readOnlyReceiver();
        readOnly.assignsFieldsMethod();  // should emit error
    }

    void testAssignsFields(@AssignsFields MethodInvocation this) {
        mutableReciever();  // should emit error
        immutableReceiver();  // should emit error
        readOnlyReceiver();
        assignsFieldsMethod();

        this.mutableReciever();  // should emit error
        this.immutableReceiver();  // should emit error
        this.readOnlyReceiver();
        this.assignsFieldsMethod();

        mutable.mutableReciever();
        mutable.immutableReceiver();  // should emit error
        mutable.readOnlyReceiver();
        mutable.assignsFieldsMethod();

        immutable.mutableReciever();  // should emit error
        immutable.immutableReceiver();
        immutable.readOnlyReceiver();
        immutable.assignsFieldsMethod();  // should emit error

        readOnly.mutableReciever();  // should emit error
        readOnly.immutableReceiver();  // should emit error
        readOnly.readOnlyReceiver();
        readOnly.assignsFieldsMethod();  // should emit error
    }

    void testExpressionMutable() {
        (new @Mutable MethodInvocation()).mutableReciever();
        (new @Mutable MethodInvocation()).immutableReceiver();  // should emit error
        (new @Mutable MethodInvocation()).readOnlyReceiver();
        (new @Mutable MethodInvocation()).assignsFieldsMethod();

        (new @Mutable MethodInvocation()).mutableReciever();
        (new @Mutable MethodInvocation()).immutableReceiver();  // should emit error
        (new @Mutable MethodInvocation()).readOnlyReceiver();
        (new @Mutable MethodInvocation()).assignsFieldsMethod();

        (true ? mutable : new @Mutable MethodInvocation()).mutableReciever();
        (true ? mutable : new @Mutable MethodInvocation()).immutableReceiver();  // should emit error
        (true ? mutable : new @Mutable MethodInvocation()).readOnlyReceiver();
        (true ? mutable : new @Mutable MethodInvocation()).assignsFieldsMethod();
    }

    void testExpressionImmutable() {
        (new @Immutable MethodInvocation()).mutableReciever();  // should emit error
        (new @Immutable MethodInvocation()).immutableReceiver();
        (new @Immutable MethodInvocation()).readOnlyReceiver();
        (new @Immutable MethodInvocation()).assignsFieldsMethod();  // should emit error

        (new @Immutable MethodInvocation()).mutableReciever();  // should emit error
        (new @Immutable MethodInvocation()).immutableReceiver();
        (new @Immutable MethodInvocation()).readOnlyReceiver();
        (new @Immutable MethodInvocation()).assignsFieldsMethod();  // should emit error

        (true ? immutable : new @Immutable MethodInvocation()).mutableReciever();  // should emit error
        (true ? immutable : new @Immutable MethodInvocation()).immutableReceiver();
        (true ? immutable : new @Immutable MethodInvocation()).readOnlyReceiver();
        (true ? immutable : new @Immutable MethodInvocation()).assignsFieldsMethod();  // should emit error
    }

    void testExpressionReadOnly() {
        (true ? mutable : new @Immutable MethodInvocation()).mutableReciever();  // should emit error
        (true ? mutable : new @Immutable MethodInvocation()).immutableReceiver(); // should emit error
        (true ? mutable : new @Immutable MethodInvocation()).readOnlyReceiver();
        (true ? mutable : new @Immutable MethodInvocation()).assignsFieldsMethod();  // should emit error

        (true ? immutable : new @Mutable MethodInvocation()).mutableReciever();  // should emit error
        (true ? immutable : new @Mutable MethodInvocation()).immutableReceiver(); // should emit error
        (true ? immutable : new @Mutable MethodInvocation()).readOnlyReceiver();
        (true ? immutable : new @Mutable MethodInvocation()).assignsFieldsMethod();  // should emit error
    }


    public static <A, B> java.util.Map<A, B> forMap(
            java.util.Map<? super A, ? extends B> map, final B defaultValue) {
        return forMap(map, null);
    }

    void testAnonClasses(@ReadOnly MethodInvocation this) {
        @I MethodInvocation m = new @I MethodInvocation();
        @I MethodInvocation n = new @I MethodInvocation() {};
    }
}
