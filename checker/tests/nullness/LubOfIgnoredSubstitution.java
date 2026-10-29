import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// The least upper bound of a type with an ignored substitution and a type with real annotations at
// that position.  B = U comes from a use with an explicit annotation, so it ignores annotations,
// and the component type of arrOf's result is an ignored substitution.  In the least upper bound
// with @Nullable U[], the component type is @Nullable U, which is no longer ignored.
public class LubOfIgnoredSubstitution {
  static class Box<T> {}

  static <B extends @Nullable Object> B[] arrOf(Box<@NonNull B> b, B other) {
    throw new RuntimeException();
  }

  static <B extends @Nullable Object> List<? extends B> listOf(Box<@NonNull B> b, B other) {
    throw new RuntimeException();
  }

  static <S extends @Nullable Object> S pick2(S a, S b) {
    return a;
  }

  static <U extends @Nullable Object> void test(
      Box<@NonNull U> nonNullU,
      @NonNull U nn,
      @Nullable U nble,
      @Nullable U[] nullableArr,
      @NonNull U[] nonNullArr,
      List<? extends @Nullable U> nullableList) {
    // :: error: (dereference.of.nullable)
    pick2(arrOf(nonNullU, nn), nullableArr)[0].toString();
    // :: error: (dereference.of.nullable)
    pick2(nullableArr, arrOf(nonNullU, nn))[0].toString();
    @Nullable U[] r1 = pick2(arrOf(nonNullU, nn), nullableArr);
    // :: error: (dereference.of.nullable)
    r1[0].toString();
    @Nullable U[] r2 = pick2(nullableArr, arrOf(nonNullU, nn));
    // :: error: (dereference.of.nullable)
    r2[0].toString();
    pick2(arrOf(nonNullU, nn), nonNullArr)[0].toString();

    // :: error: (dereference.of.nullable)
    pick2(listOf(nonNullU, nn), nullableList).get(0).toString();
    // :: error: (dereference.of.nullable)
    pick2(nullableList, listOf(nonNullU, nn)).get(0).toString();

    // Both inputs have an ignored substitution at the component type.
    pick2(arrOf(nonNullU, nn), arrOf(nonNullU, nn))[0].toString();
    // TODO: This should be a dereference.of.nullable error.  The least upper bound keeps the origin
    // of only the first input, so substituting instantiations into it gives the component type the
    // instantiation of only the first input's B, which is @NonNull U.
    // :: error: (argument)
    pick2(arrOf(nonNullU, nn), arrOf(nonNullU, nble))[0].toString();
    // :: error: (dereference.of.nullable)
    pick2(arrOf(nonNullU, nble), arrOf(nonNullU, nn))[0].toString();
  }
}
