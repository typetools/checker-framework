import java.util.List;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// B = U comes from a use with an explicit annotation, so it ignores annotations, and B's
// annotations come from `other`.  In apply(() -> list(nonNullU, nble)), list is called in the body
// of a lambda, and its result is the lambda's return expression.
public class LambdaReturnOfIgnoredPosition {
  static class Box<T> {}

  static <B extends @Nullable Object> List<? extends B> list(Box<@NonNull B> b, B other) {
    throw new RuntimeException();
  }

  static <T extends @Nullable Object> T first(List<? extends T> l) {
    throw new RuntimeException();
  }

  static <T extends @Nullable Object> T apply(Supplier<? extends List<? extends T>> s) {
    throw new RuntimeException();
  }

  static <U extends @Nullable Object> void test(Box<@NonNull U> nonNullU, @Nullable U nble) {
    // :: error: (dereference.of.nullable)
    first(list(nonNullU, nble)).toString();
    // TODO: The return and type.arguments.not.inferred errors are false positives.  B is
    // @Nullable U, so T is @Nullable U too.
    // :: error: (dereference.of.nullable)
    // :: error: (return) :: error: (type.arguments.not.inferred)
    apply(() -> list(nonNullU, nble)).toString();
  }
}
