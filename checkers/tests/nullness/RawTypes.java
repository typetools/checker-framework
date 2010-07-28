import checkers.nullness.quals.*;
import java.util.*;
@checkers.quals.DefaultQualifier("Nullable")
class RawTypes {

    class Bad {
        @NonNull String field;

        public Bad() {
            //:: (method.invocation.invalid)
            this.init();                                // error
            //:: (method.invocation.invalid)
            init();                                     // error
            this.field = "field";                       // valid
            //:: (assignment.type.incompatible)
            this.field = null;                          // error
        }

        void init() {
            output(this.field.length());    // valid
        }
    }

    class A {
        @NonNull String field;

        public A() {
            this.field = "field";                               // valid
            init();                                             // valid
        }

        public void init() @Raw {
            //:: (dereference.of.nullable)
            output(this.field.length());             // error
        }
    }

    class B extends A {
        @NonNull String otherField;

        public B() {
            super();
            //:: (assignment.type.incompatible)
            this.otherField = null;                             // error
            this.otherField = "otherField";                     // valid
        }

        @Override
        public void init() @Raw {
            //:: (dereference.of.nullable)
            output(this.field.length());            // error (TODO: substitution)
            //output(field.length());                 // error (TODO: substitution)
            //:: (dereference.of.nullable)
            output(this.otherField.length());       // error
            //output(otherField.length());            // error
            super.init();                                       // valid
        }

        void other() {
            init();                                             // valid
            this.init();                                        // valid
        }

        void otherRaw() @Raw {
            init();                                             // valid
            this.init();                                        // valid
        }
}

    class C extends B {

        @NonNull String[] strings;

        @Override
        public void init() @Raw {
            //:: (dereference.of.nullable)
            output(this.strings.length);            // error
            System.out.println();                   // valid
        }

    }

    void output(Object o) @Raw {

    }

    class D extends C {
        @Override
        public void init() @Raw {
            this.field = "s";
            output(this.field.length());
        }
    }

    class MyTest {
        int i;
        void myTest() @Raw {
            i++;
        }
    }

    class AllFieldsInitialized {
        long elapsedMillis = 0;
        long startTime = 0;

        // If all fields have an initializer, then the type of "this"
        // should be non-raw in the constructor.
        public AllFieldsInitialized() {
            nonRawMethod();
        }

        public void nonRawMethod() {
        }
    }

    class AFSIICell {
        AllFieldsSetInInitializer afsii;
    }

    class AllFieldsSetInInitializer {
        long elapsedMillis;
        long startTime;

        // If all fields have an initializer, then the type of "this"
        // should be non-raw in the constructor.
        public AllFieldsSetInInitializer() {
            elapsedMillis = 0;
            //:: (method.invocation.invalid)
            nonRawMethod();     // error
            startTime = 0;
            nonRawMethod();     // no error
            new AFSIICell().afsii = this;
        }

        public AllFieldsSetInInitializer(boolean b) {
            //:: (method.invocation.invalid)
            nonRawMethod();     // error
        }

        public void nonRawMethod() {
        }
    }

    class ConstructorInvocations {
        int v;
        public ConstructorInvocations(int v) {
            this.v = v;
        }
        public ConstructorInvocations() {
            this(0);
            nonRawMethod(); // valid
        }
        public void nonRawMethod() { }
    }

    class MethodAccess {
        public MethodAccess() {
            @NonNull String s = string();
        }

        public @NonNull String string() @Raw {
            return "nonnull";
        }
    }

    void cast(@Raw Object... args) {

        @SuppressWarnings("rawtypes")
        //:: (assignment.type.incompatible)
        Object[] argsNonRaw1 = args;

        @SuppressWarnings("cast")
        Object[] argsNonRaw2 = (Object[]) args;

    }

    class RawAfterConstructor {
        int a;
        // Should err, because some variables are not yet initialized when
        // the constructor exits.
        RawAfterConstructor() {
        }
    }


//     // TODO: reinstate.  This shows desired features, for initialization in
//     // a helper method rather than in the constructor.
//     class InitInHelperMethod {
//         int a;
//         int b;
// 
//         InitInHelperMethod(short constructor_inits_ab) {
//             a = 1;
//             b = 1;
//             nonRawMethod();
//         }
// 
//         InitInHelperMethod(boolean constructor_inits_a) {
//             a = 1;
//             init_b();
//             nonRawMethod();
//         }
// 
//         void init_b() @Raw {
//             b = 2;
//             nonRawMethod();
//         }
// 
//         InitInHelperMethod(int constructor_inits_none) {
//             init_ab();
//             nonRawMethod();
//         }
// 
//         void init_ab() @Raw {
//             a = 1;
//             b = 2;
//             nonRawMethod();
//         }
// 
//         void nonRawMethod() { }
//     }

}
