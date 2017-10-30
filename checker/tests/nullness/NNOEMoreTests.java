import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

class NNOEMoreTests {
    class NNOEMain {
        protected @Nullable String nullable = null;
        @Nullable String otherNullable = null;

        @RequiresNonNull("nullable")
        void test1() {
            nullable.toString();
        }

        @RequiresNonNull("xxx")
        // :: error: (flowexpr.parse.error)
        void test2() {
            // :: error: (dereference.of.nullable)
            nullable.toString();
        }
    }

    class NNOESeparate {
        void call1(NNOEMain p) {
            // :: error: (contracts.precondition.not.satisfied)
            p.test1();

            Object xxx = new Object();
            // :: error: (flowexpr.parse.error)
            p.test2();
        }

        void call2(NNOEMain p) {
            p.nullable = "";
            p.test1();
        }
    }

    @Nullable Object field1;

    @RequiresNonNull("field1")
    void methWithIf1() {
        if (5 < 99) {
        } else {
            field1.hashCode();
        }
    }
}
