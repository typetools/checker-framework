import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// T = U comes from a use with an explicit annotation, so T's instantiation ignores annotations, and
// S's upper bound Map<T, S> is not proper until S is resolved.  S is resolved by capture, and the
// fresh type variable must keep that upper bound, with T's instantiation applied to it.
public class RecursiveBoundOfWeakInstantiation {
  static class Box<T extends @Nullable Object> {}

  static <T extends @Nullable Object, S extends Map<T, S>> S recursive(Box<@NonNull T> b) {
    throw new RuntimeException();
  }

  static <T extends @Nullable Object, S extends Map<T, S>> S recursiveNoAnno(Box<T> b) {
    throw new RuntimeException();
  }

  static <U extends @Nullable Object> void test(
      Box<@NonNull U> nonNullU, Box<@Nullable U> nullableU) {
    recursive(nonNullU);
    // :: error: (argument)
    recursive(nullableU);
    recursiveNoAnno(nonNullU);
    Object o = recursive(nonNullU);
    recursive(nonNullU).size();
  }
}
