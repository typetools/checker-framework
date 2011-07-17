import checkers.nullness.quals.*;

import java.util.*;

class NNOEStaticFields {
    static @Nullable String nullable = null;
    static @Nullable String otherNullable = null;

//    @NonNullOnEntry("nullable")
//    void testF() {
//        nullable.toString();
//    }

    @NonNullOnEntry("NNOEStaticFields.nullable")
    void testF2() {
        nullable.toString();
    }

//    @NonNullOnEntry("nullable")
//    void testF3() {
//        NNOEStaticFields.nullable.toString();
//    }
//
//    @NonNullOnEntry("NNOEStaticFields.nullable")
//    void testF4() {
//        NNOEStaticFields.nullable.toString();
//    }
//
//    class Inner {
//        void m1(NNOEStaticFields out) {
//                NNOEStaticFields.nullable = "haha!";
//                out.testF4();
//        }
//
//        @NonNullOnEntry("NNOEStaticFields.nullable")
//        void m2(NNOEStaticFields out) {
//                out.testF4();
//        }
//    }
//
//
//    //:: error: (field.not.found.nullness.parse.error)
//    @NonNullOnEntry("NoClueWhatThisShouldBe") void testF5() {
//        //:: error: (dereference.of.nullable)
//        NNOEStaticFields.nullable.toString();
//    }
//
//    void trueNegative() {
//        //:: error: (dereference.of.nullable)
//        nullable.toString();
//        //:: error: (dereference.of.nullable)
//        otherNullable.toString();
//    }
//
//    @NonNullOnEntry("nullable")
//    void test1() {
//        nullable.toString();
//        //:: error: (dereference.of.nullable)
//        otherNullable.toString();
//    }
//
//    @NonNullOnEntry("otherNullable")
//    void test2() {
//        //:: error: (dereference.of.nullable)
//        nullable.toString();
//        otherNullable.toString();
//    }
//
//    @NonNullOnEntry({"nullable", "otherNullable"})
//    void test3() {
//        nullable.toString();
//        otherNullable.toString();
//    }
//
//    @NonNullOnEntry("System.out")
//    void test4() {
//        @NonNull Object f = System.out;
//    }

    ///////////////////////////////////////////////////////////////////////////
    /// Copied from Daikon's ChicoryPremain
    ///

    static class ChicoryPremain1 {

        // Non-null if doPurity == true
        private static @LazyNonNull Set<String> pureMethods = null;

        private static boolean doPurity = false;

        @SuppressWarnings("nullness") // dependent:  pureMethods is non-null if doPurity is true
        @AssertNonNullIfTrue("ChicoryPremain1.pureMethods")
        public static boolean shouldDoPurity() {
            return doPurity;
        }

        @NonNullOnEntry("ChicoryPremain1.pureMethods")
        public static Set<String> getPureMethods() {
            return Collections.unmodifiableSet(pureMethods);
        }

    }

    static class ClassInfo1 {
        // As of 7/6/2011, an error occurs iff the @AssertNonNullIfTrue 
        // contains "ChicoryPremain1.", regardless of whether the
        // @NonNullOnEntry contains "ChicoryPremain1.".  This is a bug.
        @SuppressWarnings("nonnullonentry.precondition.not.satisfied") // XXX TODO FIXME
        public void initViaReflection() {
            if (ChicoryPremain1.shouldDoPurity()) {
                for (String pureMeth: ChicoryPremain1.getPureMethods()) {
                }
            }
        }
    }

}
