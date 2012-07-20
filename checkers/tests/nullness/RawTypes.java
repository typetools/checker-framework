// Note that this file is a near duplicate in /nullness and /nullness-uninit

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
            field = "field";                            // valid
            //:: error: (assignment.type.incompatible)
            field = null;                               // error
        }

        void init() {
            output(this.field.length());    // valid
        }
    }

    class A {
        @NonNull String field;

        public A() {
            this.field = "field";                               // valid
            field = "field";                                    // valid
            this.init();                                        // valid
            init();                                             // valid
        }

        public void init(@Raw A this) {
            //:: error: (dereference.of.nullable)
            output(this.field.length());
        }

        public void initExpl2(@Raw A this) {
            //:: error: (argument.type.incompatible)
            output(this.field);
        }

        public void initImpl1(@Raw A this) {
            //:: error: (dereference.of.nullable)
            output(field.length());
        }

        public void initImpl2(@Raw A this) {
            //:: error: (argument.type.incompatible)
            output(field);
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
        public void init(@Raw B this) {
            //:: error: (dereference.of.nullable)
            output(this.field.length());            // error (TODO: substitution)
            super.init();                                       // valid
        }

        public void initImpl1(@Raw B this) {
            //:: error: (dereference.of.nullable)
            output(field.length());                 // error (TODO: substitution)
        }

        public void initExpl2(@Raw B this) {
            //:: error: (dereference.of.nullable)
            output(this.otherField.length());       // error
        }

        public void initImpl2(@Raw B this) {
            //:: error: (dereference.of.nullable)
            output(otherField.length());            // error
        }

        void other() {
            init();                                             // valid
            this.init();                                        // valid
        }

        void otherRaw(@Raw B this) {
            init();                                             // valid
            this.init();                                        // valid
        }
    }

    class C extends B {

        @NonNull String[] strings;

        @Override
        public void init(@Raw C this) {
            //:: error: (dereference.of.nullable)
            output(this.strings.length);            // error
            System.out.println();                   // valid
        }

    }

    // To test whether the argument is @NonNull and @NonRaw
    static void output(@NonNull Object o) { }

    class D extends C {
        @Override
        public void init(@Raw D this) {
            this.field = "s";
            output(this.field.length());
        }
    }

    class MyTest {
        int i;
        MyTest(int i) {
            this.i = i;
        }
        void myTest(@Raw MyTest this) {
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
            nonRawMethod();
            startTime = 0;
            nonRawMethod();     // no error
            new AFSIICell().afsii = this;
        }

        public AllFieldsSetInInitializer(boolean b) {
            nonRawMethod();
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

        public @NonNull String string(@Raw MethodAccess this) {
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

    // default qualifier is @Nullable, so this is OK.
    class RawAfterConstructorBad {
        Object o;
        RawAfterConstructorBad() {
        }
    }

    class RawAfterConstructorOK1 {
        @Nullable Object o;
        RawAfterConstructorOK1() {
        }
    }

    class RawAfterConstructorOK2 {
        int a;
        RawAfterConstructorOK2() {
        }
    }



// skip-test
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
//         void init_b(@Raw InitInHelperMethod this) {
//             b = 2;
//             nonRawMethod();
//         }
//
//         InitInHelperMethod(int constructor_inits_none) {
//             init_ab();
//             nonRawMethod();
//         }
//
//         void init_ab(@Raw InitInHelperMethod this) {
//             a = 1;
//             b = 2;
//             nonRawMethod();
//         }
//
//         void nonRawMethod() { }
//     }

}
