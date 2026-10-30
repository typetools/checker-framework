import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// An ignored substitution nested inside another ignored substitution.  B = U comes from a use with
// an explicit annotation, so it ignores annotations, and so does A's lower bound List<B>.  So in
// pick2's lower bound Box<List<B'>>, both the List position and B' within it are ignored.
public class NestedIgnoredSubstitution {
  static class Box<T> {}

  static <B extends @Nullable Object> List<B> listOf(Box<@NonNull B> b, B other) {
    throw new RuntimeException();
  }

  static <A extends @Nullable Object> Box<A> boxOf(@NonNull A a) {
    throw new RuntimeException();
  }

  static <S extends @Nullable Object> S pick2(S a, S b) {
    return a;
  }

  static <U extends @Nullable Object> void test(
      Box<@NonNull U> nonNullU,
      @Nullable U nble,
      @NonNull U nn,
      Box<List<@Nullable U>> nullableList,
      Box<List<@NonNull U>> nonNullList) {
    Box<List<@Nullable U>> r1 = boxOf(listOf(nonNullU, nble));
    Box<List<@Nullable U>> r2 = pick2(boxOf(listOf(nonNullU, nble)), nullableList);
    Box<List<@Nullable U>> r3 = pick2(nullableList, boxOf(listOf(nonNullU, nble)));
    pick2(boxOf(listOf(nonNullU, nble)), nullableList);
    pick2(nullableList, boxOf(listOf(nonNullU, nble)));
    Box<List<@NonNull U>> r4 = pick2(boxOf(listOf(nonNullU, nn)), nonNullList);
    pick2(boxOf(listOf(nonNullU, nn)), nonNullList);
    // :: error: (argument)
    pick2(boxOf(listOf(nonNullU, nble)), nonNullList);
  }
}
