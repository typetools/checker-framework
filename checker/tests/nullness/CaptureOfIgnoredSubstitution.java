import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// Capture conversion of a type with an ignored substitution in the bound of a wildcard.  B = U
// comes from a use with an explicit annotation, so it ignores annotations, and the bound of the
// wildcard in the result of listOf or listOfN is an ignored substitution.
public class CaptureOfIgnoredSubstitution {
  static class Box<T> {}

  static <B extends @Nullable Object> List<? extends B> listOf(Box<@NonNull B> b, B other) {
    throw new RuntimeException();
  }

  static <B extends @Nullable Object> List<? extends B> listOfN(Box<@Nullable B> b, B other) {
    throw new RuntimeException();
  }

  static <E extends @Nullable Object> E get0(List<? extends E> l) {
    throw new RuntimeException();
  }

  static <U extends @Nullable Object> void test(
      Box<@NonNull U> nonNullU, Box<@Nullable U> nullableU, @NonNull U nn, @Nullable U nble) {
    // :: error: (dereference.of.nullable)
    get0(listOf(nonNullU, nble)).toString();
    // TODO: This is a false positive.  Capture conversion of the argument's type makes the ignored
    // substitution the upper bound of a captured type variable, and the result of capture
    // conversion does not record ignored substitutions, so the ignored annotation, @Nullable, is
    // compared as a real annotation.
    // :: error: (dereference.of.nullable)
    get0(listOfN(nullableU, nn)).toString();

    // :: error: (dereference.of.nullable)
    listOf(nonNullU, nble).get(0).toString();
    listOfN(nullableU, nn).get(0).toString();
  }
}
