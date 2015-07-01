package java.util;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.dataflow.qual.Pure;

import java.util.Comparator;
import java.util.function.Function;

// Javadoc says: "a comparator may optionally permit comparison of null
// arguments, while maintaining the requirements for an equivalence relation."
public interface Comparator<T extends @Nullable Object> {
    public abstract int compare(T a1, T a2);
    @Pure public abstract boolean equals(@Nullable Object a1);

    public static <T extends Comparable<@NonNull ? super @NonNull T>> Comparator<T> naturalOrder(){
        throw new RuntimeException("skeleton method");
    }

    public static <T> Comparator<@Nullable T> nullsFirst(Comparator<@Nullable ? super T> comparator) {
        throw new RuntimeException("skeleton method");
    }

    public static <T> Comparator<@Nullable T> nullsLast(Comparator<@Nullable ? super T> comparator) {
        throw new RuntimeException("skeleton method");
    }

    public static <T, U extends Comparable<? super U>> Comparator<T> comparing(
            Function<? super T, ? extends U> keyExtractor) {
        throw new RuntimeException("skeleton method");
    }
}
