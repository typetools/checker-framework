import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.Pure;

class RequiresNonNullTest {

    @Nullable Object field1;
    @Nullable Object field2;

    @RequiresNonNull("field1")
    void method1() {
        field1.toString(); // OK, field1 is known to be non-null
        this.field1.toString(); // OK, field1 is known to be non-null
        // :: error: (dereference.of.nullable)
        field2.toString(); // error, might throw NullPointerException
    }

    @RequiresNonNull("field1")
    void method1also() {
        // ok, precondition satisfied by NNOE
        method1();
    }

    void method2() {
        field1 = new Object();
        method1(); // OK, satisfies method precondition
        field1 = null;
        // :: error: (contracts.precondition.not.satisfied)
        method1(); // error, does not satisfy method precondition
    }

    protected @Nullable Object field;

    @RequiresNonNull("field")
    public void requiresNonNullField() {}

    public void clientFail(RequiresNonNullTest arg1) {
        // :: error: (contracts.precondition.not.satisfied)
        arg1.requiresNonNullField();
    }

    public void clientOK(RequiresNonNullTest arg2) {
        arg2.field = new Object();
        // note that the following line works
        @NonNull Object o = arg2.field;

        arg2.requiresNonNullField(); // OK, field is known to be non-null
    }

    // TODO: forbid the field in @NNOE to be less visible than the method

    protected static @Nullable Object staticfield;

    @Pure
    @RequiresNonNull("staticfield")
    // :: warning: (purity.deterministic.void.method)
    public void reqStaticName() {
        reqStaticQualName();
    }

    @Pure
    @RequiresNonNull("RequiresNonNullTest.staticfield")
    // :: warning: (purity.deterministic.void.method)
    public void reqStaticQualName() {
        reqStaticName();
    }

    public void statClientOK(RequiresNonNullTest arg1) {
        staticfield = new Object();
        arg1.reqStaticName();

        staticfield = new Object();
        arg1.reqStaticQualName();

        RequiresNonNullTest.staticfield = new Object();
        arg1.reqStaticName();
        RequiresNonNullTest.staticfield = new Object();
        arg1.reqStaticQualName();
    }

    public void statClientFail(RequiresNonNullTest arg1) {
        // :: error: (contracts.precondition.not.satisfied)
        arg1.reqStaticName();
        // :: error: (contracts.precondition.not.satisfied)
        arg1.reqStaticQualName();
    }

    class NNOESubTest extends RequiresNonNullTest {
        public void subClientOK(NNOESubTest arg3) {
            arg3.field = new Object();
            arg3.requiresNonNullField();
        }

        public void subClientFail(NNOESubTest arg4) {
            // :: error: (contracts.precondition.not.satisfied)
            arg4.requiresNonNullField();
        }

        public void subStat(NNOESubTest arg5) {
            RequiresNonNullTest.staticfield = new Object();
            arg5.reqStaticQualName();

            staticfield = new Object();
            arg5.reqStaticQualName();

            NNOESubTest.staticfield = new Object();
            arg5.reqStaticQualName();
        }
    }

    private @Nullable Object notHidden;

    class NNOEHidingTest extends RequiresNonNullTest {

        protected @Nullable String field;

        public void hidingClient1(NNOEHidingTest arg5) {
            arg5.field = "ha!";
            /* We should be testing that the Object "field" from the superclass
             * is non-null. We currently only match on the field name and do not
             * handle hiding correctly. Instead, we output an error, if we
             * detect that hiding happened.
             * TODO: correctly resolve hidden fields.
             */
            arg5.requiresNonNullField();
        }

        public void hidingClient2(NNOEHidingTest arg6) {
            // :: error: (contracts.precondition.not.satisfied)
            arg6.requiresNonNullField();
        }

        protected @Nullable Object notHidden;

        @RequiresNonNull("notHidden")
        void notHiddenTest() {
            // the field in the superclass is private -> don't complain about hiding
        }
    }

    static @Nullable Object o = "m";

    @RequiresNonNull("o")
    void test() {
        o = null;
    }

    @RequiresNonNull("thisShouldIssue1Error")
    // Test case for Issue 1051
    // https://github.com/typetools/checker-framework/issues/1051
    // :: error: (flowexpr.parse.error)
    void testIssue1051() {}
}
