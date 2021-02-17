import org.checkerframework.checker.nullness.qual.*;

// TODO: Add as test
public class RefineArray {
    public static <T> T[] concat(T @Nullable [] a, T @Nullable [] b) {
        if (a == null) {
            if (b != null) {
                return b;
            } else {
                @SuppressWarnings("unchecked")
                T[] result = (T[]) new Object[0];
                return result;
            }
        } else {
            if (b == null) {
                return a;
            } else {
                @SuppressWarnings("unchecked")
                T[] result = (T[]) new @MonotonicNonNull Object[a.length + b.length];

                System.arraycopy(a, 0, result, 0, a.length);
                System.arraycopy(b, 0, result, a.length, b.length);
                return result;
            }
        }
    }
}
