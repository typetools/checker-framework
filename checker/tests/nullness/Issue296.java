// Test case for Issue 296:
// https://github.com/typetools/checker-framework/issues/296

import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// Note that with -AinvariantArrays we would get additional errors.
public class Issue296 {
  public static <T> void f1(T[] a) {
    @Nullable T[] r1 = Arrays.copyOf(a, a.length + 1);
    // :: error: (argument.type.incompatible)
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
    // :: error: (argument.type.incompatible)
    @Nullable T[] r2 = Arrays.<@NonNull T>copyOf(a, a.length + 1);
    @Nullable T[] r3 = Arrays.<@Nullable T>copyOf(a, a.length + 1);
  }
}
