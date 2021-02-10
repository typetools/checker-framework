// Test case for issue #574: https://github.com/typetools/checker-framework/issues/574

import org.checkerframework.checker.initialization.qual.UnderInitialization;

@SuppressWarnings({
    // A warning is issued that fields are not initialized in the constructor.
    // That is expected and it is not what is being verified in this test.
    "initialization.field.uninitialized",
    // Normally @UnknownInitialization is the only initialization annotation allowed on fields.
    // However, for the purposes of this test, fields must be annotated with @UnderInitialization.
    "initialization.invalid.field.type"
})
public class Issue574 {
    @UnderInitialization(Object.class) Object o1;

    @UnderInitialization(String.class) Object o2;

    @UnderInitialization(Character.class) Object o3;

    @UnderInitialization(Number.class) Object o4;

    @UnderInitialization(Double.class) Object o5;

    @UnderInitialization(Integer.class) Object o6;

    @UnderInitialization(CharSequence.class) Object i1; // CharSequence is an interface

    void testLubOfClasses(boolean flag) {
        @UnderInitialization(Object.class) Object l1 = flag ? o2 : o3;
        @UnderInitialization(Number.class) Object l2 = flag ? o5 : o6;

        @UnderInitialization(Object.class) Object l3 = flag ? o1 : o2;
        @UnderInitialization(Object.class) Object l4 = flag ? o1 : o3;

        @UnderInitialization(Number.class) Object l5 = flag ? o4 : o5;
        @UnderInitialization(Number.class) Object l6 = flag ? o4 : o6;

        // :: error: (assignment.type.incompatible)
        @UnderInitialization(Character.class) Object l7 = flag ? o1 : o2;
        // :: error: (assignment.type.incompatible)
        @UnderInitialization(Integer.class) Object l8 = flag ? o4 : o5;
    }

    void testLubOfClassesAndInterfaces(boolean flag) {
        @UnderInitialization(Object.class) Object l1 = flag ? i1 : o3;

        @UnderInitialization(Object.class) Object l2 = flag ? o1 : i1;
        @UnderInitialization(Object.class) Object l3 = flag ? o1 : o3;

        // :: error: (assignment.type.incompatible)
        @UnderInitialization(Character.class) Object l4 = flag ? o1 : i1;
    }
}
