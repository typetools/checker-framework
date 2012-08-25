import checkers.quals.*;
import checkers.nullness.quals.*;

/**
 * The component type of newly created arrays is always @Nullable,
 * also for boxed types.
 * This is an expanded version of the test case for Issue 151:
 * http://code.google.com/p/checker-framework/issues/detail?id=151
 */
public class ArrayCreationNullable {

    void testObjectArray(@NonNull Object @NonNull [] p) {
        @NonNull Object @NonNull [] objs;
        //:: error: (assignment.type.incompatible)
        objs = new Object [10];
        objs[0].toString();
        //:: error: (assignment.type.incompatible)
        objs = new @Nullable Object [10];
        objs[0].toString();
        //:: error: (type.invalid)
        objs = new @NonNull Object [10];
        objs[0].toString();
        // Allowed.
        objs = p;
        objs[0].toString();
    }

    @DefaultQualifier(NonNull.class)
    void testObjectArray2() {
        Object [] objs;
        // Even if the default qualifier is NonNull, array component
        // types must be Nullable.
        //:: error: (assignment.type.incompatible)
        objs = new Object [10];
        objs[0].toString();
    }

    void testInitializers() {
        Object []  objs = { 1, 2, 3 };
        objs = new Integer[] { 1, 2, 3 };
        objs = new Object [] { new Object(), "ha" };

        @NonNull Object [] objs2;// = {};
        //:: error: (assignment.type.incompatible)
        objs2 = new Integer[] { 1, null, 3 };
        //:: error: (assignment.type.incompatible)
        objs2 = new Object[] { new Object(), "ha", null };

        @NonNull Object [] objs3 = new Integer[] { 1, 2, 3 };
        objs3 = new Integer[] { 1, 2, 3 };
        //:: error: (assignment.type.incompatible)
        objs3 = new Integer[] { 1, 2, 3, null };

        (new Integer[] { 1, 2, 3 })[0].toString();
        //:: error: (dereference.of.nullable)
        (new Integer[] { 1, 2, 3, null })[0].toString();
    }

    void testStringArray(@NonNull String @NonNull [] p) {
        @NonNull String @NonNull [] strs;
        //:: error: (assignment.type.incompatible)
        strs = new String [10];
        strs[0].toString();
        //:: error: (assignment.type.incompatible)
        strs = new @Nullable String [10];
        strs[0].toString();
        //:: error: (type.invalid)
        strs = new @NonNull String [10];
        strs[0].toString();
        // Allowed.
        strs = p;
        strs[0].toString();
    }

    void testIntegerArray(@NonNull Integer @NonNull [] p) {
        @NonNull Integer @NonNull [] ints;
        //:: error: (assignment.type.incompatible)
        ints = new Integer [10];
        ints[0].toString();
        //:: error: (assignment.type.incompatible)
        ints = new @Nullable Integer [10];
        ints[0].toString();
        //:: error: (type.invalid)
        ints = new @NonNull Integer [10];
        ints[0].toString();
        // Allowed.
        ints = p;
        ints[0].toString();
    }

    // The component type of zero-length arrays can
    // be non-null - they will always generate
    // IndexOutOfBoundsExceptions, but are usually just
    // used for the type, e.g. in List.toArray.
    void testLengthZero() {
        @NonNull Object @NonNull [] objs;
        objs = new Object[0];
    }

    Object[] oa = new Object[] {new Object()};
    //:: error: (assignment.type.incompatible)
    Object[] oa2 = new Object[] {new Object(), null};

    public static void main(String[] args) {
        ArrayCreationNullable e = new ArrayCreationNullable();
        Integer[] ints = new Integer[] { 5, 6 };
        // This would result in a NPE, if there were no error.
        e.testIntegerArray(ints);
    }

}
