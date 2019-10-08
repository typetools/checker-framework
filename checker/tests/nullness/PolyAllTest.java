import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.PolyAll;

public class PolyAllTest<T extends Comparable<T>> {

    @Pure
    public int compare(@PolyAll T[] a1, @PolyAll T[] a2) {
        if (a1 == a2) {
            return 0;
        }
        int len = Math.min(a1.length, a2.length);
        for (int i = 0; i < len; i++) {
            T elt1 = a1[i];
            T elt2 = a2[i];
            // Make null compare smaller than anything else
            if ((elt1 == null) && (elt2 == null)) {
                continue;
            }
            if (elt1 == null) {
                return -1;
            }
            if (elt2 == null) {
                return 1;
            }
            int tmp = elt1.compareTo(elt2);
            if (tmp != 0) {
                return tmp;
            }
            // Check the assumption that the two elements are equal.
            assert elt1.equals(elt2);
        }
        return a1.length - a2.length;
    }

    void test(
            PolyAllTest<@NonNull String> nonnull,
            @NonNull String[] nonnullArray,
            @Nullable String[] nullableArray) {
        nonnull.compare(nonnullArray, nonnullArray);
        nonnull.compare(nullableArray, nonnullArray);
        nonnull.compare(nullableArray, nullableArray);
    }
}
