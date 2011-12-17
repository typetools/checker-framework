import checkers.nullness.quals.*;
import java.util.*;
@checkers.quals.DefaultQualifier("Nullable")
class RawTypes {

    class Bad {
        @NonNull String field;

        public Bad() {
            //:: error: (method.invocation.invalid.rawness)
            this.init();                                // error
            //:: error: (method.invocation.invalid.rawness)
            init();                                     // error
            this.field = "field";                       // valid
            //:: error: (assignment.type.incompatible)
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
            //:: error: (dereference.of.nullable)
            output(this.field.length());             // error
        }
    }

    class B extends A {
        @NonNull String otherField;

        public B() {
            super();
            //:: error: (assignment.type.incompatible)
            this.otherField = null;                             // error
            this.otherField = "otherField";                     // valid
        }

        @Override
        public void init() @Raw {
            //:: error: (dereference.of.nullable)
            output(this.field.length());            // error (TODO: substitution)
            //output(field.length());                 // error (TODO: substitution)
            //:: error: (dereference.of.nullable)
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

    //:: warning: (fields.uninitialized)
    class C extends B {

        @NonNull String[] strings;

        @Override
        public void init() @Raw {
            //:: error: (dereference.of.nullable)
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
        MyTest(int i) {
            this.i = i;
        }
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

    //:: warning: (fields.uninitialized)
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
            //:: warning: (method.invocation.invalid.rawness)
            nonRawMethod();     // error
            startTime = 0;
            nonRawMethod();     // no error
            new AFSIICell().afsii = this;
        }

        //:: warning: (fields.uninitialized)
        public AllFieldsSetInInitializer(boolean b) {
            //:: warning: (method.invocation.invalid.rawness)
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
        //:: error: (assignment.type.incompatible)
        Object[] argsNonRaw1 = args;

        @SuppressWarnings("cast")
        Object[] argsNonRaw2 = (Object[]) args;

    }

    class RawAfterConstructorBad {
        Object o;
        //:: warning: (fields.uninitialized)
        RawAfterConstructorBad() {
        }
    }

    class RawAfterConstructorOK1 {
        @Nullable Object o;
        //:: warning: (fields.uninitialized)
        RawAfterConstructorOK1() {
        }
    }

    class RawAfterConstructorOK2 {
        int a;
        //:: warning: (fields.uninitialized)
        RawAfterConstructorOK2() {
        }
    }



     // TODO: reinstate.  This shows desired features, for initialization in
     // a helper method rather than in the constructor.
    class InitInHelperMethod {
        int a;
        int b;

        InitInHelperMethod(short constructor_inits_ab) {
            a = 1;
            b = 1;
            nonRawMethod();
        }

        InitInHelperMethod(boolean constructor_inits_a) {
            a = 1;
            init_b();
            nonRawMethod();
        }

        // @SuppressWarnings because initialization is computed only for the
        // constructor.  It should arguably be computed for every raw reference.
        @SuppressWarnings("rawness")
        @NonNullOnEntry("a")
        @AssertNonNullAfter("b")
        void init_b() @Raw {
            b = 2;
            nonRawMethod();
        }

        InitInHelperMethod(int constructor_inits_none) {
            init_ab();
            nonRawMethod();
        }

        // @SuppressWarnings because initialization is computed only for the
        // constructor.  It should arguably be computed for every raw reference.
        @SuppressWarnings("rawness")
        @AssertNonNullAfter({"a", "b"})
        void init_ab() @Raw {
            a = 1;
            b = 2;
            nonRawMethod();
        }

        void nonRawMethod() { }
    }

}
