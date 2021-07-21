import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * The component type of newly created arrays is always @Nullable, also for boxed types. This is an
 * expanded version of the test case for Issue 151:
 * https://github.com/typetools/checker-framework/issues/151
 */
public class ArrayCreationNullable {

    void testObjectArray(@NonNull Object @NonNull [] p) {
        @NonNull Object @NonNull [] objs;
        // :: error: (new.array.type.invalid)
        objs = new Object[10];
        objs[0].toString();
        // :: error: (assignment.type.incompatible)
        objs = new @Nullable Object[10];
        objs[0].toString();
        // :: error: (new.array.type.invalid)
        objs = new @NonNull Object[10];
        objs[0].toString();
        // Allowed.
        objs = p;
        objs[0].toString();
    }

    @DefaultQualifier(NonNull.class)
    void testObjectArray2() {
        Object[] objs;
        // Even if the default qualifier is NonNull, array component
        // types must be Nullable.
        // :: error: (new.array.type.invalid)
        objs = new Object[10];
        objs[0].toString();
    }

    void testInitializers() {
        Object[] objs = {1, 2, 3};
        objs = new Integer[] {1, 2, 3};
        objs = new Object[] {new Object(), "ha"};

        @NonNull Object[] objs2 = {};
        // :: error: (assignment.type.incompatible)
        objs2 = new Integer[] {1, null, 3};
        // :: error: (assignment.type.incompatible)
        objs2 = new Object[] {new Object(), "ha", null};

        @NonNull Object[] objs3 = new Integer[] {1, 2, 3};
        objs3 = new Integer[] {1, 2, 3};
        // :: error: (assignment.type.incompatible)
        objs3 = new Integer[] {1, 2, 3, null};

        (new Integer[] {1, 2, 3})[0].toString();
        // :: error: (dereference.of.nullable)
        (new Integer[] {1, 2, 3, null})[0].toString();

        // The assignment context is used to infer a @Nullable component type.
        @Nullable Object[] objs4 = new Integer[] {1, 2, 3};
        // :: error: (dereference.of.nullable)
        objs4[0].toString();
        objs4 = new Integer[] {1, 2, 3};
    }

    void testStringArray(@NonNull String @NonNull [] p) {
        @NonNull String @NonNull [] strs;
        // :: error: (new.array.type.invalid)
        strs = new String[10];
        strs[0].toString();
        // :: error: (assignment.type.incompatible)
        strs = new @Nullable String[10];
        strs[0].toString();
        // :: error: (new.array.type.invalid)
        strs = new @NonNull String[10];
        strs[0].toString();
        // Allowed.
        strs = p;
        strs[0].toString();
    }

    void testIntegerArray(@NonNull Integer @NonNull [] p) {
        @NonNull Integer @NonNull [] ints;
        // :: error: (new.array.type.invalid)
        ints = new Integer[10];
        ints[0].toString();
        // :: error: (assignment.type.incompatible)
        ints = new @Nullable Integer[10];
        ints[0].toString();
        // :: error: (new.array.type.invalid)
        ints = new @NonNull Integer[10];
        ints[0].toString();
        // Allowed.
        ints = p;
        ints[0].toString();
    }

    // The component type of zero-length arrays can be non-null - they will always generate
    // IndexOutOfBoundsExceptions, but are usually just used for the type, e.g. in List.toArray.
    void testLengthZero() {
        @NonNull Object @NonNull [] objs;
        objs = new Object[0];
    }

    /* Test case for Issue 153.
    // toArray re-uses the passed array, if it is of appropriate size.
    // It is only guaranteed to be non-null, if it is at most the same size.
    void testToArray(java.util.Set<Object> nns) {
        @NonNull Object [] nna = nns.toArray(new Object[nns.size()]);
        // Given array is too small -> new one is created.
        nna = nns.toArray(new Object[nns.size()-2]);
        // Padding elements will be null.
        // TODO:: error: (assignment.type.incompatible)
        nna = nns.toArray(new Object[nns.size() + 2]);
        @Nullable Object [] nbla = nns.toArray(new Object[nns.size() + 2]);
    }
    */

    void testMultiDim() {
        // new double[10][10] has type double @NonNull[] @Nullable[]
        // :: error: (new.array.type.invalid)
        double @NonNull [] @NonNull [] daa = new double[10][10];
        double @NonNull [] @Nullable [] daa2 = new double[10][10];

        // new Object[10][10] has type @Nullable Object @NonNull[] @Nullable[]
        // :: error: (new.array.type.invalid)
        @Nullable Object @NonNull [] @NonNull [] oaa = new Object[10][10];
        @Nullable Object @NonNull [] @Nullable [] oaa2 = new Object[10][10];

        // new Object[10][10] has type @Nullable Object @NonNull[] @Nullable[]
        // :: error: (new.array.type.invalid)
        oaa2 = new Object @NonNull [10] @NonNull [10];

        @MonotonicNonNull Object @NonNull [] @MonotonicNonNull [] oaa3 =
                new @MonotonicNonNull Object @NonNull [10] @MonotonicNonNull [10];
        oaa3[0] = new @MonotonicNonNull Object[4];
        // :: error: (assignment.type.incompatible)
        oaa3[0] = null;
        // :: error: (assignment.type.incompatible) :: error: (accessing.nullable)
        oaa3[0][0] = null;
    }

    @PolyNull Object[] testPolyNull(@PolyNull Object[] in) {
        @PolyNull Object[] out = new @PolyNull Object[in.length];
        for (int i = 0; i < in.length; ++i) {
            if (in[i] == null) {
                out[i] = null;
            } else {
                out[i] = in[i].getClass().toString();
                // :: error: (assignment.type.incompatible)
                out[i] = null;
            }
        }
        return out;
    }

    void testMonotonicNonNull() {
        @MonotonicNonNull Object @NonNull [] loa = new @MonotonicNonNull Object @NonNull [10];
        loa = new Object @NonNull [10];
        loa[0] = new Object();
        @MonotonicNonNull Object @NonNull [] loa2 = new Object @NonNull [10];
        // :: error: (dereference.of.nullable)
        loa2[0].toString();
    }

    @MonotonicNonNull Object @NonNull [] testReturnContext() {
        return new Object[10];
    }

    // :: error: (new.array.type.invalid)
    @NonNull Object @NonNull [] oa0 = new Object[10];

    // OK
    @MonotonicNonNull Object @NonNull [] loa0 = new @MonotonicNonNull Object @NonNull [10];

    Object[] oa1 = new Object[] {new Object()};

    // :: error: (assignment.type.incompatible)
    Object[] oa2 = new Object[] {new Object(), null};

    public static void main(String[] args) {
        ArrayCreationNullable e = new ArrayCreationNullable();
        Integer[] ints = new Integer[] {5, 6};
        // This would result in a NPE, if there were no error.
        e.testIntegerArray(ints);
    }
}
