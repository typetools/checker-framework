import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.PolyAll;

public class PolyAllTest<T extends Comparable<T>> {

    @Pure
    public int comparePolyAll(@PolyAll T[] a1, @PolyAll T[] a2) {
        return 0;
    }

    @Pure
    public int compare(@PolyNull T[] a1, @PolyNull T[] a2) {
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

        nonnull.comparePolyAll(nonnullArray, nonnullArray);
        nonnull.comparePolyAll(nullableArray, nonnullArray);
        nonnull.comparePolyAll(nullableArray, nullableArray);
    }

    <T> @PolyAll T polyAll(@PolyAll T param, @PolyAll T param2) {
        return param;
    }

    <T> @PolyNull T polyNull(@PolyNull T param, @PolyNull T param2) {
        return param;
    }

    public static boolean flag;

    <S> S use(S s, @NonNull S nonNullS) {
        if (flag) {
            // :: error: (return.type.incompatible)
            return this.<S>polyNull(s, nonNullS);
        }
        // :: error: (return.type.incompatible)
        return this.<S>polyAll(s, nonNullS);
    }
}
