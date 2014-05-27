// Test case for Issue 296:
// https://code.google.com/p/checker-framework/issues/detail?id=296
import java.util.Arrays;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;

// Note that with -AinvariantArrays we would get additional errors.
class Issue296 {
    public static <T> void f1(T[] a) {
        @Nullable T[] r1 = Arrays.copyOf(a, a.length + 1);
        //:: error: (argument.type.incompatible)
        @Nullable T[] r2 = Arrays.<@NonNull T>copyOf(a, a.length + 1);
        @Nullable T[] r3 = Arrays.<@Nullable T>copyOf(a, a.length + 1);
    }

    public static <T> void f2(@NonNull T[] a) {
        @Nullable T[] r1 = Arrays.copyOf(a, a.length + 1);
        @Nullable T[] r2 = Arrays.<@NonNull T>copyOf(a, a.length + 1);
        @Nullable T[] r3 = Arrays.<@Nullable T>copyOf(a, a.length + 1);
    }

    public static <T> void f3(@Nullable T[] a) {
        @Nullable T[] r1 = Arrays.copyOf(a, a.length + 1);
        //:: error: (argument.type.incompatible)
        @Nullable T[] r2 = Arrays.<@NonNull T>copyOf(a, a.length + 1);
        @Nullable T[] r3 = Arrays.<@Nullable T>copyOf(a, a.length + 1);
    }
}
