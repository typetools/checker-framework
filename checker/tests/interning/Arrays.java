import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.interning.qual.PolyInterned;

import java.util.ArrayList;
import java.util.List;

public class Arrays {

    public static Integer[] arrayclone_simple(Integer[] a_old) {
        int len = a_old.length;
        Integer[] a_new = new Integer[len];
        for (int i = 0; i < len; i++) {
            a_new[i] = Integer.valueOf(a_old[i]); // valid
        }
        return a_new;
    }

    public static void test(@Interned Integer i, @Interned String s) {
        String @Interned [] iarray1 = new String @Interned [2];
        String @Interned [] iarray2 = new String @Interned [] {"foo", "bar"};
        // :: error: (assignment.type.incompatible)
        s = iarray1[1]; // error

        String[] sa = new String[22];
        // :: error: (assignment.type.incompatible)
        iarray1 = sa; // error
        sa = iarray1; // OK

        @Interned String[] istrings1 = new @Interned String[2];
        @Interned String[] istrings2 = new @Interned String[] {"foo", "bar"};
        s = istrings1[1]; // OK

        @Interned String @Interned [][] multi1 = new @Interned String @Interned [2][3];
        @Interned String @Interned [][] multi2 = new @Interned String @Interned [2][];
    }

    public final @Interned class InternedClass {}

    private static InternedClass[] returnToArray() {
        List<InternedClass> li = new ArrayList<>();
        return li.toArray(new InternedClass[li.size()]);
    }

    private static void sortIt() {
        java.util.Arrays.sort(new InternedClass[22]);
    }

    private @Interned String[] elts_String;

    public @Interned String min_elt() {
        return elts_String[0];
    }

    private double @Interned [] @Interned [] elts_da;

    public void add_mod_elem(double @Interned [] v, int count) {
        elts_da[0] = v;
    }

    public static @PolyInterned Object[] subarray(
            @PolyInterned Object[] a, int startindex, int length) {
        @PolyInterned Object[] result = new @PolyInterned Object[length];
        System.arraycopy(a, startindex, result, 0, length);
        return result;
    }

    public static void trim(int len) {
        @Interned Object @Interned [] vals = null;
        @Interned Object[] new_vals = subarray(vals, 0, len);
    }

    public static @Interned Object @Interned [] internSubsequence(
            @Interned Object @Interned [] seq, int start, int end) {
        @Interned Object[] subseq_uninterned = subarray(seq, start, end - start);
        return null;
    }
}
