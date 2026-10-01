// Test case for https://github.com/typetools/checker-framework/issues/2172

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.PolyNull;

public class Issue2172 {
  public static @PolyNull Integer @PolyNull [] makeArr(@PolyNull Integer n) {
    return new @PolyNull Integer @PolyNull [] {n};
  }

  public static void f(@NonNull Integer n) {
    for (Integer i : makeArr(n)) {
      for (Integer j : makeArr(i)) {
        @NonNull Integer m = j;
      }
    }
  }
}
