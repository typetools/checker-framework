// From issue #216:
// https://code.google.com/p/checker-framework/issues/detail?id=216

public class InferMethod {
        public abstract static class Generic<T> {
                public class Nested {
                        public void nestedMethod(T item) { }
                }

                public static class NestedStatic<TInner> {
                        public void nestedMethod2(TInner item) { }
                }

// DISABLED_UNTIL_DFF_MERGE
//                public abstract void method();
// END_DISABLED_UNTIL_DFF_MERGE
                public abstract void method2();
                public void method3(T item) { }
        }

        public static class Concrete extends Generic<String> {

// DISABLED_UNTIL_DFF_MERGE
//
// The following test fails for both the main Checker Framework and the DFF branch,
// although it is only present in the DFF branch.  Since the root cause is unrelated
// to the org.checkerframework.dataflow framework, we have disabled the test and will re-enable it after
// merging DFF with the main Checker Framework.
//
//                @Override
//                public void method() {
//                        Nested o = new Nested();
//                        o.nestedMethod("test");         // ERROR        found: String, required T extends Object
//                                                                                // Expected no error message
//                }
// END_DISABLED_UNTIL_DFF_MERGE

                @Override
                public void method2() {
                        NestedStatic<String> o = new NestedStatic<>();
                        o.nestedMethod2("test");        // Compiles as expected

                        this.method3("test");           // Compiles as expected
                }

        }
}
