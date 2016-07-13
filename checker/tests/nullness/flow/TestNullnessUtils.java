import org.checkerframework.checker.nullness.NullnessUtils;
import org.checkerframework.checker.nullness.qual.*;

/**
 * Test class org.checkerframework.checker.nullness.NullnessUtils.
 */
//@non-308-skip-test
class TestNullnessUtils {
    void testRef1(@Nullable Object o) {
        // one way to use as a cast:
        @NonNull Object l1 = NullnessUtils.castNonNull(o);
    }

    void testRef2(@Nullable Object o) {
        // another way to use as a cast:
        NullnessUtils.castNonNull(o).toString();
    }

    void testRef3(@Nullable Object o) {
        // use as statement:
        NullnessUtils.castNonNull(o);
        o.toString();
    }

    void testArr1(@Nullable Object @NonNull [] a) {
        // one way to use as a cast:
        @NonNull Object[] l2 = NullnessUtils.castNonNullDeep(a);
        // Careful, the non-deep version only casts the main modifier.
        //:: error: (assignment.type.incompatible)
        @NonNull Object[] l2b = NullnessUtils.castNonNull(a);
        // OK
        @Nullable Object[] l2c = NullnessUtils.castNonNull(a);
    }

    void testArr1b(@Nullable Object @Nullable [] a) {
        // one way to use as a cast:
        @NonNull Object[] l2 = NullnessUtils.castNonNullDeep(a);
        // Careful, the non-deep version only casts the main modifier.
        //:: error: (assignment.type.incompatible)
        @NonNull Object[] l2b = NullnessUtils.castNonNull(a);
        // OK
        @Nullable Object[] l2c = NullnessUtils.castNonNull(a);
    }

    void testArr2(@Nullable Object @NonNull [] a) {
        // another way to use as a cast:
        NullnessUtils.castNonNullDeep(a)[0].toString();
    }

    void testArr3(@Nullable Object @NonNull [] a) {
        // use as statement:
        NullnessUtils.castNonNullDeep(a);
        a.toString();
        // TODO: @EnsuresNonNull cannot express that
        // all the array components are non-null.
        // a[0].toString();
    }

    /*
    // TODO: flow does not propagate component types.
    void testArr3(@Nullable Object @NonNull [] a) {
        // one way to use as a statement:
        NullnessUtils.castNonNull(a);
        a[0].toString();
    }
    */

    // TODO:
    // // Flow should refine @MonotonicNonNull component types to @NonNull.
    // // This is a prerequisite for issue 154 (or for workarounds to issue 154).
    // void testArr4(@NonNull Object @NonNull [] nno1, @MonotonicNonNull Object @NonNull [] lnno1) {
    //     @MonotonicNonNull Object [] lnno2;
    //     @NonNull Object [] nno2;
    //     nno2 = nno1;
    //     lnno2 = lnno1;
    //     lnno2 = nno1;
    //     //:: error: (assignment.type.incompatible)
    //     nno2 = lnno1;
    //     lnno2 = NullnessUtils.castNonNullDeep(nno1);
    //     nno2 = NullnessUtils.castNonNullDeep(lnno1);
    //     lnno2 = NullnessUtils.castNonNullDeep(nno1);
    //     nno2 = NullnessUtils.castNonNullDeep(lnno1);
    // }

    // TODO:
    // // Flow should refine @MonotonicNonNull component types to @NonNull.
    // // This is a prerequisite for issue 154 (or for workarounds to issue 154).
    // void testArr5(@MonotonicNonNull Object @NonNull [] a) {
    //     @MonotonicNonNull Object [] l5 = NullnessUtils.castNonNullDeep(a);
    //     @NonNull Object [] l6 = l5;
    //     @NonNull Object [] l7 = NullnessUtils.castNonNullDeep(a);
    // }

    void testMultiArr1(@Nullable Object @NonNull [] @Nullable [] a) {
        //:: error: (assignment.type.incompatible) :: error: (accessing.nullable)
        @NonNull Object l3 = a[0][0];
        // one way to use as a cast:
        @NonNull Object[][] l4 = NullnessUtils.castNonNullDeep(a);
    }

    void testMultiArr2(@Nullable Object @NonNull [] @Nullable [] a) {
        // another way to use as a cast:
        NullnessUtils.castNonNullDeep(a)[0][0].toString();
    }

    void testMultiArr3(@Nullable Object @Nullable [] @Nullable [] @Nullable [] a) {
        //:: error: (dereference.of.nullable) :: error: (accessing.nullable)
        a[0][0][0].toString();
        // another way to use as a cast:
        NullnessUtils.castNonNullDeep(a)[0][0][0].toString();
    }

    public static void main(String[] args) {
        Object[] @Nullable [] err = new Object[10][10];
        Object[][] e1 = NullnessUtils.castNonNullDeep(err);
        e1[0][0].toString();
    }
}
