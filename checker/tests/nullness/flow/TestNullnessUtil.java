import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.util.NullnessUtil;

/** Test class org.checkerframework.checker.nullness.util.NullnessUtil. */
public class TestNullnessUtil {
  void testRef1(@Nullable Object o) {
    // one way to use as a cast:
    @NonNull Object l1 = NullnessUtil.castNonNull(o);
  }

  void testRef2(@Nullable Object o) {
    // another way to use as a cast:
    NullnessUtil.castNonNull(o).toString();
  }

  void testRef3(@Nullable Object o) {
    // use as statement:
    NullnessUtil.castNonNull(o);
    o.toString();
  }

  void testArr1(@Nullable Object @NonNull [] a) {
    // one way to use as a cast:
    @NonNull Object[] l2 = NullnessUtil.castNonNullDeep(a);
    // Careful, the non-deep version only casts the main modifier.
    // :: error: (assignment)
    @NonNull Object[] l2b = NullnessUtil.castNonNull(a);
    // OK
    @Nullable Object[] l2c = NullnessUtil.castNonNull(a);
  }

  void testArr1b(@Nullable Object @Nullable [] a) {
    // one way to use as a cast:
    @NonNull Object[] l2 = NullnessUtil.castNonNullDeep(a);
    // Careful, the non-deep version only casts the main modifier.
    // :: error: (assignment)
    @NonNull Object[] l2b = NullnessUtil.castNonNull(a);
    // OK
    @Nullable Object[] l2c = NullnessUtil.castNonNull(a);
  }

  void testArr2(@Nullable Object @NonNull [] a) {
    // another way to use as a cast:
    NullnessUtil.castNonNullDeep(a)[0].toString();
  }

  void testArr3(@Nullable Object @NonNull [] a) {
    // use as statement:
    NullnessUtil.castNonNullDeep(a);
    a.toString();
    // TODO: @EnsuresNonNull cannot express that
    // all the array components are non-null.
    // a[0].toString();
  }

  /*
  // TODO: flow does not propagate component types.
  void testArr3(@Nullable Object @NonNull [] a) {
      // one way to use as a statement:
      NullnessUtil.castNonNull(a);
      a[0].toString();
  }
  */

  void testMultiArr1(@Nullable Object @NonNull [] @Nullable [] a) {
    // :: error: (assignment) :: error: (accessing.nullable)
    @NonNull Object l3 = a[0][0];
    // one way to use as a cast:
    @NonNull Object[][] l4 = NullnessUtil.castNonNullDeep(a);
  }

  void testMultiArr2(@Nullable Object @NonNull [] @Nullable [] a) {
    // another way to use as a cast:
    NullnessUtil.castNonNullDeep(a)[0][0].toString();
  }

  void testMultiArr3(@Nullable Object @Nullable [] @Nullable [] @Nullable [] a) {
    // :: error: (dereference.of.nullable) :: error: (accessing.nullable)
    a[0][0][0].toString();
    // another way to use as a cast:
    NullnessUtil.castNonNullDeep(a)[0][0][0].toString();
  }

  public static void main(String[] args) {
    Object[] @Nullable [] err = new Object[10][10];
    Object[][] e1 = NullnessUtil.castNonNullDeep(err);
    e1[0][0].toString();
  }
}
