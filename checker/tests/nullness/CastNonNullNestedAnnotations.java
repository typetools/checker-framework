import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.util.NullnessUtil;

// castNonNull's parameter and result are uses of T with explicit primary annotations, so T's bounds
// from the argument and from the assignment context ignore primary annotations, but not nested
// ones.  If the nested annotations of the argument and of the assignment context conflict, then
// inference fails as well as the assignment.
public class CastNonNullNestedAnnotations {
  void arrays(
      @Nullable Object @Nullable [] nullableElts, @NonNull Object @Nullable [] nonNullElts) {
    @Nullable Object[] a1 = NullnessUtil.castNonNull(nullableElts);
    @NonNull Object[] a2 = NullnessUtil.castNonNull(nonNullElts);
    @Nullable Object[] a3 = NullnessUtil.castNonNull(nonNullElts);
    // :: error: (assignment) :: error: (type.arguments.not.inferred)
    @NonNull Object[] a4 = NullnessUtil.castNonNull(nullableElts);
    // :: error: (dereference.of.nullable)
    NullnessUtil.castNonNull(nullableElts)[0].toString();
  }

  void lists(@Nullable List<@Nullable String> nullableElts, @Nullable List<String> nonNullElts) {
    List<@Nullable String> l1 = NullnessUtil.castNonNull(nullableElts);
    List<String> l2 = NullnessUtil.castNonNull(nonNullElts);
    // :: error: (assignment) :: error: (type.arguments.not.inferred)
    List<String> l3 = NullnessUtil.castNonNull(nullableElts);
    // :: error: (dereference.of.nullable)
    NullnessUtil.castNonNull(nullableElts).get(0).toString();
  }
}
