// Test case for issue #986:
// https://github.com/typetools/checker-framework/issues/986

// @skip-test until the issue is fixed

public class Issue986 {

    public static void main(String[] args) {
        String array[] = new String[3];
        array[0].length(); // NPE here
    }

    // Flow should refine @MonotonicNonNull component types to @NonNull.
    void testArr4(@NonNull Object @NonNull [] nno1, @MonotonicNonNull Object @NonNull [] lnno1) {
        @MonotonicNonNull Object[] lnno2;
        @NonNull Object[] nno2;
        nno2 = nno1;
        lnno2 = lnno1;
        lnno2 = nno1;
        // :: error: (assignment.type.incompatible)
        nno2 = lnno1;
        lnno2 = NullnessUtil.castNonNullDeep(nno1);
        nno2 = NullnessUtil.castNonNullDeep(lnno1);
        lnno2 = NullnessUtil.castNonNullDeep(nno1);
        nno2 = NullnessUtil.castNonNullDeep(lnno1);
    }

    // Flow should refine @MonotonicNonNull component types to @NonNull.
    // This is a prerequisite for issue #986 (or for workarounds to issue #986).
    void testArr5(@MonotonicNonNull Object @NonNull [] a) {
        @MonotonicNonNull Object[] l5 = NullnessUtil.castNonNullDeep(a);
        @NonNull Object[] l6 = l5;
        @NonNull Object[] l7 = NullnessUtil.castNonNullDeep(a);
    }
}
