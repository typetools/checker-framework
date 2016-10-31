import org.checkerframework.checker.nullness.NullnessUtil;
import org.checkerframework.checker.nullness.qual.*;

/**
 * Test class org.checkerframework.checker.nullness.NullnessUtil.
 */
//@non-308-skip-test
class TestNullnessUtil {
    void testRef1(@Nullable Object o) {
        // one way to use as a cast:
        @NonNull Object l1 = NullnessUtil.castNonNull(o);
    }

    void testRef2(@Nullable Object o) {
        // another way to use as a cast:
        NullnessUtil.castNonNull(o).toString();
    }

    void testRef3(@Nullable Object o) {
        // use as statement:
        NullnessUtil.castNonNull(o);
        o.toString();
    }

    void testArr1(@Nullable Object @NonNull [] a) {
        // one way to use as a cast:
        @NonNull Object[] l2 = NullnessUtil.castNonNullDeep(a);
        // Careful, the non-deep version only casts the main modifier.
        //:: error: (assignment.type.incompatible)
        @NonNull Object[] l2b = NullnessUtil.castNonNull(a);
        // OK
        @Nullable Object[] l2c = NullnessUtil.castNonNull(a);
    }

    void testArr1b(@Nullable Object @Nullable [] a) {
        // one way to use as a cast:
        @NonNull Object[] l2 = NullnessUtil.castNonNullDeep(a);
        // Careful, the non-deep version only casts the main modifier.
        //:: error: (assignment.type.incompatible)
        @NonNull Object[] l2b = NullnessUtil.castNonNull(a);
        // OK
        @Nullable Object[] l2c = NullnessUtil.castNonNull(a);
    }

    void testArr2(@Nullable Object @NonNull [] a) {
        // another way to use as a cast:
        NullnessUtil.castNonNullDeep(a)[0].toString();
    }

    void testArr3(@Nullable Object @NonNull [] a) {
        // use as statement:
        NullnessUtil.castNonNullDeep(a);
        a.toString();
        // TODO: @EnsuresNonNull cannot express that
        // all the array components are non-null.
        // a[0].toString();
    }

    /*
    // TODO: flow does not propagate component types.
    void testArr3(@Nullable Object @NonNull [] a) {
        // one way to use as a statement:
        NullnessUtil.castNonNull(a);
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
    //     lnno2 = NullnessUtil.castNonNullDeep(nno1);
    //     nno2 = NullnessUtil.castNonNullDeep(lnno1);
    //     lnno2 = NullnessUtil.castNonNullDeep(nno1);
    //     nno2 = NullnessUtil.castNonNullDeep(lnno1);
    // }

    // TODO:
    // // Flow should refine @MonotonicNonNull component types to @NonNull.
    // // This is a prerequisite for issue 154 (or for workarounds to issue 154).
    // void testArr5(@MonotonicNonNull Object @NonNull [] a) {
    //     @MonotonicNonNull Object [] l5 = NullnessUtil.castNonNullDeep(a);
    //     @NonNull Object [] l6 = l5;
    //     @NonNull Object [] l7 = NullnessUtil.castNonNullDeep(a);
    // }

    void testMultiArr1(@Nullable Object @NonNull [] @Nullable [] a) {
        //:: error: (assignment.type.incompatible) :: error: (accessing.nullable)
        @NonNull Object l3 = a[0][0];
        // one way to use as a cast:
        @NonNull Object[][] l4 = NullnessUtil.castNonNullDeep(a);
    }

    void testMultiArr2(@Nullable Object @NonNull [] @Nullable [] a) {
        // another way to use as a cast:
        NullnessUtil.castNonNullDeep(a)[0][0].toString();
    }

    void testMultiArr3(@Nullable Object @Nullable [] @Nullable [] @Nullable [] a) {
        //:: error: (dereference.of.nullable) :: error: (accessing.nullable)
        a[0][0][0].toString();
        // another way to use as a cast:
        NullnessUtil.castNonNullDeep(a)[0][0][0].toString();
    }

    public static void main(String[] args) {
        Object[] @Nullable [] err = new Object[10][10];
        Object[][] e1 = NullnessUtil.castNonNullDeep(err);
        e1[0][0].toString();
    }
}
