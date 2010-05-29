import checkers.oigj.quals.*;

/**
 * Simple tests to verify the receiver subtyping tests (a.k.a.
 * invocability tests)
 *
 */
class MethodInvocation {
    void mutable() @Mutable {}
    void immutable() @Immutable {}
    void readOnly() @ReadOnly {}
    void assignsFields() @AssignsFields {}

    @ReadOnly MethodInvocation readonly;
    @Mutable MethodInvocation mutable;
    @Immutable MethodInvocation immutable;

    void testReadOnly() {
        readonly.readOnly();
        mutable.readOnly();
        immutable.readOnly();
    }

    void testMutable() {
        //:: (method.invocation.invalid)
        readonly.mutable();
        mutable.mutable();
        //:: (method.invocation.invalid)
        immutable.mutable();
    }

    void testImmutable() {
        //:: (method.invocation.invalid)
        readonly.immutable();
        //:: (method.invocation.invalid)
        mutable.immutable();
        immutable.immutable();
    }

    void testAssignsFields() {
        //:: (method.invocation.invalid)
        readonly.assignsFields();
        mutable.assignsFields();
        //:: (method.invocation.invalid)
        immutable.assignsFields();
    }

    void selfReadOnly() @ReadOnly {
        this.readOnly();
        //:: (method.invocation.invalid)
        this.mutable();
        //:: (method.invocation.invalid)
        this.immutable();
        //:: (method.invocation.invalid)
        this.assignsFields();
    }

    void selfMutable() @Mutable {
        this.readOnly();
        this.mutable();
        //:: (method.invocation.invalid)
        this.immutable();
        this.assignsFields();
    }

    void selfImmutable() @Immutable {
        this.readOnly();
        //:: (method.invocation.invalid)
        this.mutable();
        this.immutable();
        //:: (method.invocation.invalid)
        this.assignsFields();
    }

    void selfAssignsFields() @AssignsFields {
        this.readOnly();
        //:: (method.invocation.invalid)
        this.mutable();
        //:: (method.invocation.invalid)
        this.immutable();
        this.assignsFields();
    }
}
