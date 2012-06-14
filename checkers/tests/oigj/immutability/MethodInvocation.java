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
        //:: error: (method.invocation.invalid)
        readonly.mutable();
        mutable.mutable();
        //:: error: (method.invocation.invalid)
        immutable.mutable();
    }

    void testImmutable() {
        //:: error: (method.invocation.invalid)
        readonly.immutable();
        //:: error: (method.invocation.invalid)
        mutable.immutable();
        immutable.immutable();
    }

    void testAssignsFields() {
        //:: error: (method.invocation.invalid)
        readonly.assignsFields();
        mutable.assignsFields();
        //:: error: (method.invocation.invalid)
        immutable.assignsFields();
    }

    void selfReadOnly() @ReadOnly {
        this.readOnly();
        //:: error: (method.invocation.invalid)
        this.mutable();
        //:: error: (method.invocation.invalid)
        this.immutable();
        //:: error: (method.invocation.invalid)
        this.assignsFields();
    }

    void selfMutable() @Mutable {
        this.readOnly();
        this.mutable();
        //:: error: (method.invocation.invalid)
        this.immutable();
        this.assignsFields();
    }

    void selfImmutable() @Immutable {
        this.readOnly();
        //:: error: (method.invocation.invalid)
        this.mutable();
        this.immutable();
        //:: error: (method.invocation.invalid)
        this.assignsFields();
    }

    void selfAssignsFields() @AssignsFields {
        this.readOnly();
        //:: error: (method.invocation.invalid)
        this.mutable();
        //:: error: (method.invocation.invalid)
        this.immutable();
        this.assignsFields();
    }
}
