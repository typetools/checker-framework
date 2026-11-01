import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// A wildcard with a primary annotation whose bound is an ignored substitution.  B = U comes from a
// use with an explicit annotation, so it ignores annotations, and the bound of the wildcard in the
// result of nnList or nbleList is an ignored substitution.  B's annotations come from `other`.
public class AnnotatedWildcardOfIgnoredSubstitution {
  static class Box<T> {}

  static <B extends @Nullable Object> List<@NonNull ? extends B> nnList(
      Box<@NonNull B> b, B other) {
    throw new RuntimeException();
  }

  static <B extends @Nullable Object> List<@Nullable ? extends B> nbleList(
      Box<@NonNull B> b, B other) {
    throw new RuntimeException();
  }

  static <S extends @Nullable Object> S id(S s) {
    return s;
  }

  static <S extends @Nullable Object> S pick2(S a, S b) {
    return a;
  }

  static <U extends @Nullable Object> void test(
      Box<@NonNull U> nonNullU,
      @NonNull U nn,
      @Nullable U nble,
      List<? extends @Nullable U> nullableList,
      List<? extends @NonNull U> nonNullList) {
    // :: error: (dereference.of.nullable)
    id(nnList(nonNullU, nble)).get(0).toString();
    // TODO: This is a false positive.  B is @NonNull U, so the upper bound of the wildcard is too.
    // :: error: (dereference.of.nullable)
    id(nnList(nonNullU, nn)).get(0).toString();
    // :: error: (argument)
    List<? extends @NonNull U> a1 = id(nnList(nonNullU, nble));
    List<? extends @Nullable U> a2 = id(nnList(nonNullU, nble));
    // :: error: (dereference.of.nullable)
    id(nbleList(nonNullU, nn)).get(0).toString();
    // :: error: (dereference.of.nullable)
    pick2(nnList(nonNullU, nn), nullableList).get(0).toString();
    // :: error: (dereference.of.nullable)
    pick2(nnList(nonNullU, nble), nonNullList).get(0).toString();
    // TODO: This is a false positive.  See the call of id above.
    // :: error: (dereference.of.nullable)
    pick2(nnList(nonNullU, nn), nonNullList).get(0).toString();
    // :: error: (dereference.of.nullable)
    pick2(nbleList(nonNullU, nn), nonNullList).get(0).toString();
  }
}
