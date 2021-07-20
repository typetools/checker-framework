import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

import java.util.Collections;
import java.util.Set;

public class NNOEStaticFields {
    static @Nullable String nullable = null;
    static @Nullable String otherNullable = null;

    @RequiresNonNull("nullable")
    void testF() {
        nullable.toString();
    }

    @RequiresNonNull("NNOEStaticFields.nullable")
    void testF2() {
        nullable.toString();
    }

    @RequiresNonNull("nullable")
    void testF3() {
        NNOEStaticFields.nullable.toString();
    }

    @RequiresNonNull("NNOEStaticFields.nullable")
    void testF4() {
        NNOEStaticFields.nullable.toString();
    }

    class Inner {
        void m1(NNOEStaticFields out) {
            NNOEStaticFields.nullable = "haha!";
            out.testF4();
        }

        @RequiresNonNull("NNOEStaticFields.nullable")
        void m2(NNOEStaticFields out) {
            out.testF4();
        }
    }

    @RequiresNonNull("NoClueWhatThisShouldBe")
    // :: error: (flowexpr.parse.error)
    void testF5() {
        // :: error: (dereference.of.nullable)
        NNOEStaticFields.nullable.toString();
    }

    void trueNegative() {
        // :: error: (dereference.of.nullable)
        nullable.toString();
        // :: error: (dereference.of.nullable)
        otherNullable.toString();
    }

    @RequiresNonNull("nullable")
    void test1() {
        nullable.toString();
        // :: error: (dereference.of.nullable)
        otherNullable.toString();
    }

    @RequiresNonNull("otherNullable")
    void test2() {
        // :: error: (dereference.of.nullable)
        nullable.toString();
        otherNullable.toString();
    }

    @RequiresNonNull({"nullable", "otherNullable"})
    void test3() {
        nullable.toString();
        otherNullable.toString();
    }

    @RequiresNonNull("System.out")
    void test4() {
        @NonNull Object f = System.out;
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Copied from Daikon's ChicoryPremain
    ///

    static class ChicoryPremain1 {

        // Non-null if doPurity == true
        private static @MonotonicNonNull Set<String> pureMethods = null;

        private static boolean doPurity = false;

        @EnsuresNonNullIf(result = true, expression = "ChicoryPremain1.pureMethods")
        // this postcondition cannot be proved with the Checker Framework, as the relation
        // between doPurity and pureMethods is not explicit
        public static boolean shouldDoPurity() {
            // :: error: (contracts.conditional.postcondition.not.satisfied)
            return doPurity;
        }

        @RequiresNonNull("ChicoryPremain1.pureMethods")
        public static Set<String> getPureMethods() {
            return Collections.unmodifiableSet(pureMethods);
        }
    }

    static class ClassInfo1 {
        public void initViaReflection() {
            if (ChicoryPremain1.shouldDoPurity()) {
                for (String pureMeth : ChicoryPremain1.getPureMethods()) {}
            }
        }
    }
}
